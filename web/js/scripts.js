"use strict";

SVGElement.prototype.getTransformToElement = SVGElement.prototype.getTransformToElement || function(elem) {
    return elem.getScreenCTM().inverse().multiply(this.getScreenCTM());
};

/**
* Add hooks to dynamic elements
*/
$(function() {
  var menuClose = 
      "<button class='btn btn-default btn-xs popover-close close' onclick='$(this).closest(\".popover\").remove()'>&times;</button>";	
    
  /** Show a pop-over on click */
  var menu = function(provider) {
    return function() {
      var that = $(this);
      if (typeof that.data('content') === 'undefined') {
        provider(that, function(data) {
          that.data('content', menuClose + data);

          that.popover({
            animation : false,
            html : true,
            placement : 'auto bottom',
            trigger : 'click'
          });

          that.popover('show');
        })
      }
    }
  }

  var metaPopover = function(url) {
    return menu(function(that, receiver) {
      $.ajax(url + that.data("id")).done(receiver)
    })
  }

  var container = "body";

  $(container).on(
    {
      mouseenter : function() {
        $('[data-id="' + $(this).data('id') + '"]').addClass(
        'highlight');
      },
      mouseleave : function() {
        $('[data-id="' + $(this).data('id') + '"]').removeClass(
        'highlight');
      }
    }, ".object, .method, .type, .field");

    $(container).on('click', '.tree-toggle', function() {
      $(this).parent().children('.tree-children').toggle(0);
      $(this).toggleClass("tree-collapsed");
    });

    $(container).on("click", ".method", metaPopover("/meta/method/"));

    $(container).on("click", ".type", metaPopover("/meta/type/"));

    $(container).on("click", ".field", metaPopover("/meta/field/"));

    // event label menu
    $(container).on("click", ".event > .event-label", menu(function (that, receiver) {
      // locate the outer event
      var event = that.parent('.event');
      var id = event.data("id");
      var db = event.data("db");

      var item = function(q, text) {
        return "<a class='dropitem' href='/repl?q=" + q + "&db=" + db + "'>" + text + "</a>"
      }

      receiver(
        item("at(" + id + ")", "Add REPL") + 
        item("compile(at(" + id + ").seeds)", "Synthesize")
      );
    }));

    $(container).on('click', '.call-search', function() {
      var that = $(this);

      repl('synthesize(meta.method(' + that.data('m') + 'L))', 'swing');
    })

    // highlight all incoming stmt labels
    $(container).on('click', '.stmt-label', function() {
      var that = $(this);

      var code = that.parents('.code-result');
      $('.stmt-label').removeClass('highlight');
      that.addClass('highlight');
      if (that.data("in") !== "")
        that.data("in").split(" ").forEach(function (s) {
          code.find("." + s).addClass('highlight');
        });
    });

    // statement label menu
    $(container).on('click', '.stmt-label', menu(function (that, receiver) {
      $.ajax('/repl/run?db='+that.data('db')+'&q=at('+that.data('e')+').seeds').done(receiver);
    }));

    // expand seed
    $(container).on('click', '.seed', function () {
      var that = $(this);
      if (that.data('open') !== true) {
        that.data('open', true);
        $.ajax('/repl/slice?db='+that.data('db')+'&e='+that.data('e')+'&o='+that.data('o')).done(function (data) {
          that.append("<div class='seed-body'>" + data + "</div>");
        });
      }
    });
});


/** Graph drawing */
var drawGraph = function (es, ls, n, id) {
  var g = new dagreD3.graphlib.Graph();

  //Set an object for the graph label
  g.setGraph({});

  var i = 0;
  while (i < n) {
    g.setNode("v" + i, { 
      label: ls[i], 
      class: 'v' + i,
      rx: 5, 
      ry: 5 
    });
    i = i + 1;
  }

  es.forEach(function (elt) {
    g.setEdge("v" + elt._1, "v" + elt._3, { 
      label: elt._2, 
      labelType: "html",
      lineInterpolate: 'bundle'
    }); 
  });
  
    
  //Create the renderer
  var render = new dagreD3.render();

  // Set up an SVG group so that we can translate the final graph.
  var svg = d3.select("#" + id + " svg "),
      inner = svg.append("g");

  // Set up zoom support
  var zoom = d3.behavior.zoom().on("zoom", function() {
      inner.attr("transform", "translate(" + d3.event.translate + ")" +
                                  "scale(" + d3.event.scale + ")");
  });
  svg.call(zoom);

  // Run the renderer. This is what draws the final graph.
  render(inner, g);

  // show details on click
  inner.selectAll('g.node').each(function (v) {    
    $(this).click(function () {
      $('#' + id + ' .dagre-node').hide();	
      $('#' + id + ' .dagre-node.' + v).show();
    });
  });

  inner.selectAll('g.edgePath').each(function (e) {
    $(this).click(function () {
    });
  });

  // Center the graph
  var initialScale = .75;
  zoom
    .scale(initialScale)
    .event(svg);
  svg.attr('height', g.graph().height * initialScale + 100);
  svg.attr('width', g.graph().width * initialScale + 200);
}
