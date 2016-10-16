package edu.mit.csail.cap.query

import db.Database

case class Parameters(
    ConfigFile: String = "web/config.json",

    // load metadata for all traces on server start
    EagerLoadMetadata: Boolean = false,

    // apply rules to simplify output code
    Simplify: Boolean = true,

    // force inclusion of calls to seeds even if the bodies are empty
    IncludeSeeds: Boolean = true,

    // collapse type hierarchy
    CollapseTypes: Boolean = true,

    // remove unused framework constructors
    RemoveUnusedFrameworkConstructors: Boolean = true,

    // number of matches 
    Matches: Int = 1,

    // slice expansion step limit,
    ExpansionSteps: Int = 5 * 1024,

    // slice expansion timeout (ns),
    ExpansionTimeout: Long = 30L * 1000L * 1000L * 1000L,

    // maximum depth in the slice graph to explore
    CoverDepth: Int = Int.MaxValue,

    // Print source symbol names
    PrintSourceSymbols: Boolean = false,

    // Print primitive values
    PrintPrimitives: Boolean = false,

    // Print string values
    PrintStrings: Boolean = true) extends util.JSON {
  override def toString = toJSON(this)
}

object Parameters {
  var EventCacheSize: Int = 10000
}

