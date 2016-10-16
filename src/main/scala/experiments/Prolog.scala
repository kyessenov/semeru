package edu.mit.csail.cap.query
package experiments

import analysis._
import util._

object Prolog {
  /**
   * Prelude to graph search in Prolog:
   *
   * connected(A,B,T) :- edge(A,B,L,H), between(L,H,T).
   * path(A,B,[A | CB],T) :- connected(A,C,T), \+member(CB, A), path(C,B,CB,T).
   * path(A,B,[A,B],T) :- connected(A,B,T).
   *
   * Query looks like:
   * path(a,b,P,T)
   *
   */

  def atom(c: Cluster) = "n" + c.hashCode.toString.replace("-", "m")

  def writeProlog(abs: HeapAbstraction, file: String) {
    import java.io._
    val out = new FileWriter(file)
    for (
      Edge(a, r, b) <- abs;
      Segment(l, h) <- r.segments
    ) out.write("edge(" + atom(a) + "," + atom(b) + "," + l + "," + (h - 1) + ").\n")
    out.flush
    out.close
  }
}
