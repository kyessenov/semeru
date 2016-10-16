package edu.mit.csail.cap.query
package experiments

import db._
import analysis._
import ingest._

/**
 * Naming convention:
 * - full: {name}(_{feature})?
 * - demo: demo(_{name}_{feature})?
 *
 * Name does not contain "_" except for tutorials (which are handled specially)
 */

object Experiment {
  def parse(name: String) = {
    val exp = name.split("_").toList match {
      // handle swing tutorials specially
      case "demo" :: "swing" :: ws => "swing_" + ws.dropRight(1).mkString("_")
      case "swing" :: ws           => "swing_" + ws.mkString("_")
      // handle demo trace specially
      case "demo" :: Nil           => ""
      // standard naming scheme
      case "demo" :: w :: _        => w
      case w :: _                  => w
      case Nil                     => assert(false)
    }
    Experiments.All.find(_.name == exp)
  }
}

trait Experiment {
  assert(!name.startsWith("demo"))

  def name: String

  /** User packages */
  def user: Option[String]

  /** Framework packages */
  def framework: Option[String]

  /** Configuration file */
  def config: String = name

  /** Meta-data name */
  def meta: String

  def collect(config: String): Process

  def collect(demo: Boolean, log: String): Process = {
    val configFile = "config/" + (if (demo) "demo_" + config else config)
    collect(configFile)
  }

  def process(log: String): web.TraceConfig = {
    Processor.run(log, meta)
    web.TraceConfig(name = log, user = user, framework = framework)
  }

  def run(demo: Boolean, log: String): web.TraceConfig = {
    collect(demo, log).waitFor
    process(log)
  }

  def run(): web.TraceConfig = run(false, name)
}

object SwingDemo extends Experiment {
  override def name = ""
  override def collect(config: String) = ???
  override def config = "swing"
  override def meta = "meta_swing"
  override def user = None
  override def framework = Some("java.*, javax.*, apple.*, com.apple.*")
}

trait CommandExperiment extends Experiment {
  def cmd: List[String]
  override def collect(config: String) =
    Collector.launch(config, cmd)
}

trait MainExperiment extends Experiment {
  def jar: String
  def main: String
  override def collect(config: String) =
    Collector.runMain(config, main, jar)
}

case class SwingJARExperiment(name: String, jar: String, user: Option[String], framework: Option[String] = None) extends Experiment {
  override def collect(config: String) =
    Collector.runJAR(config, jar)
  override def config = "swing"
  override def meta = "meta_swing"
}

case class Tutorial(main: String) extends MainExperiment {
  assert(main.size > 0)

  def jar = "../semeru-data/swing.jar"

  override def user = Some("components.*")
  override def framework = None
  override def config = "swing"
  override def meta = "meta_swing"
  override def name = "swing_" + main.replace(".", "_")
}

object Experiments {
  val Test = new MainExperiment {
    override def name = "test"
    override def meta = "meta_test"
    override def jar = Collector.Agent
    override def main = "test.cap.Test"
    override def user = Some("test.cap.*")
    override def framework = None
  }

  val FrameworkUsage = new MainExperiment {
    override def name = "FrameworkUsage"
    override def meta = "meta_test"
    override def jar = Collector.Agent
    override def main = "test.cap.framework.FrameworkUsage"
    override def user = Some("test.cap.client.*")
    override def framework = Some("test.cap.framework.*")
  }

  val Eclipse = new CommandExperiment {
    override def name = "eclipse"
    override def meta = "meta_eclipse"
    override def cmd =
      List(System.getProperty("user.home") + "/Documents/eclipse-luna/eclipse", "-vmargs")
    override def user = 
      Some("org.eclipse.jdt.*,org.eclipse.mylyn.*,org.eclipse.ant.*,org.python.*,com.python.*,net.sourceforge.texlipse.*")
    override def framework = None
  }

  val RText = SwingJARExperiment(
    "rtext",
    "../semeru-data/rtext/RText.jar",
    Some("org.fife.*"))

  val Stocks = SwingJARExperiment(
    "stocks",
    "../semeru-data/stocks.jar",
    Some("hirondelle.*"))

  val PasswordStore = SwingJARExperiment(
    "passwordstore",
    "../semeru-data/passwordstore.jar",
    Some("passwordstore.*"))

  val Movies = SwingJARExperiment(
    "movies",
    "../semeru-data/movies.jar",
    Some("hirondelle.*"))

  val JEdit = SwingJARExperiment(
    "jedit",
    "../semeru-data/jedit/jedit.jar",
    Some("org.gjt.*"))

  val Jgnash = SwingJARExperiment(
    "jgnash",
    "../semeru-data/jgnash/jgnash2.jar",
    Some("jgnash.*"))

  val Tutorials = """components.BorderDemo
components.ButtonDemo
components.ButtonHtmlDemo
components.CheckBoxDemo
components.ColorChooserDemo
components.ColorChooserDemo2
components.ComboBoxDemo
components.ComboBoxDemo2
components.Converter
components.CustomComboBoxDemo
components.CustomIconDemo
components.DialogDemo
components.DynamicTreeDemo
components.FileChooserDemo
components.FileChooserDemo2
components.FormattedTextFieldDemo
components.FormatterFactoryDemo
components.FrameDemo
components.FrameDemo2
components.Framework
components.GenealogyExample
components.GlassPaneDemo
components.HtmlDemo
components.IconDemoApp
components.InternalFrameDemo
components.LabelDemo
components.LayeredPaneDemo
components.LayeredPaneDemo2
components.ListDemo
components.ListDialogRunner
components.MenuDemo
components.MenuGlueDemo
components.MenuLayoutDemo
components.MenuLookDemo
components.MenuSelectionManagerDemo
components.PasswordDemo
components.PopupMenuDemo
components.ProgressBarDemo
components.ProgressBarDemo2
components.ProgressMonitorDemo
components.RadioButtonDemo
components.RootLayeredPaneDemo
components.ScrollDemo
components.ScrollDemo2
components.SharedModelDemo
components.SimpleTableDemo
components.SimpleTableSelectionDemo
components.SliderDemo
components.SliderDemo2
components.SpinnerDemo
components.SpinnerDemo2
components.SpinnerDemo3
components.SpinnerDemo4
components.SplitPaneDemo
components.SplitPaneDemo2
components.SplitPaneDividerDemo
components.TabComponentsDemo
components.TabbedPaneDemo
components.TableDemo
components.TableDialogEditDemo
components.TableFTFEditDemo
components.TableFilterDemo
components.TablePrintDemo
components.TableRenderDemo
components.TableSelectionDemo
components.TableSortDemo
components.TableToolTipsDemo
components.TextAreaDemo
components.TextComponentDemo
components.TextDemo
components.TextFieldDemo
components.TextInputDemo
components.TextSamplerDemo
components.ToolBarDemo
components.ToolBarDemo2
components.TopLevelDemo
components.TreeDemo
components.TreeIconDemo
components.TreeIconDemo2
components.dnd.BasicDnD
components.dnd.ChooseDropActionDemo
components.dnd.DropDemo
components.dnd.FillViewportHeightDemo
components.dnd.ListCutPaste
components.dnd.LocationSensitiveDemo
components.dnd.TextCutPaste
components.dnd.TopLevelTransferHandlerDemo
components.events.Beeper
components.events.ComponentEventDemo
components.events.ContainerEventDemo
components.events.DocumentEventDemo
components.events.FocusEventDemo
components.events.InternalFrameEventDemo
components.events.KeyEventDemo
components.events.ListDataEventDemo
components.events.ListSelectionDemo
components.events.MouseEventDemo
components.events.MouseMotionEventDemo
components.events.MouseWheelEventDemo
components.events.MultiListener
components.events.TableListSelectionDemo
components.events.TreeExpandEventDemo
components.events.TreeExpandEventDemo2
components.events.WindowEventDemo
components.misc.AccessibleScrollDemo
components.misc.ActionDemo
components.misc.DesktopDemo
components.misc.Diva
components.misc.FieldValidator
components.misc.FocusConceptsDemo
components.misc.FocusTraversalDemo
components.misc.GradientTranslucentWindowDemo
components.misc.InputVerificationDemo
components.misc.InputVerificationDialogDemo
components.misc.ModalityDemo
components.misc.Myopia
components.misc.ShapedWindowDemo
components.misc.TapTapTap
components.misc.TrackFocusDemo
components.misc.TranslucentWindowDemo
components.misc.TrayIconDemo
components.misc.Wallpaper""".split("\n").map(_.trim).map(Tutorial).toList

  val All = List(
    Eclipse,
    RText,
    Stocks,
    PasswordStore,
    Movies,
    Test,
    FrameworkUsage,
    JEdit,
    Jgnash,
    SwingDemo) ::: Tutorials

  /** Parameters are: name, demo? (true by default), log for full (name by default) */
  def main(args: Array[String]) {
    val name = args(0)
    val experiment = All.find(_.name == name).getOrElse {
      throw new RuntimeException(s"missing experiment $name")
    }

    val (demo, log) = args.toList.tail match {
      case Nil =>
        (true, "demo")
      case "true" :: Nil =>
        (true, "demo")
      case "false" :: Nil =>
        (false, experiment.name)
      case "false" :: log :: Nil =>
        (false, log)
      case _ =>
        throw new RuntimeException("parameters not recognized")
    }

    info(s"*** Running in ${if (demo) "demo" else "full"} mode ***")
    val out = experiment.run(demo, log)
    info(s"*** Configuration: ${out}")
  }

}
