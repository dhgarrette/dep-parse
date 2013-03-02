package dhg.depparse

import dhg.util.CollectionUtil._

case class DepRel[E, W](rel: Dependency, gov: DepNode[E], dep: DepNode[W])

/**
 * A Dependency Graph holder class
 * 
 * @param relations		The set of dependency relations
 * @param sourceTree	The dependency tree from which the graph was constructed
 * @param source		The original sentence from which the graph was taken
 */
case class DepGraph[E, W](relations: Set[DepRel[E, W]], sourceTree: DepNode[String], source: String) {
  lazy val nodes = childMap.keySet
  lazy val childMap = groupRelations(relations)

  private[this] def groupRelations(relations: Set[DepRel[E, W]]) = relations.groupBy((_: DepRel[E, W]).gov).mapVals(_.map(r => (r.rel, r.dep)))

  def graphviz =
    ("digraph G {" +:
      relations.toVector.map {
        case DepRel(reln, DepNode(gov, _, govTag, govIdx, _), DepNode(dep, _, depTag, depIdx, _)) =>
          """  "%s-%s-%s" -> "%s-%s-%s" [ label = "%s" ]""".format(gov, govTag, govIdx, dep, depTag, depIdx, reln)
      } :+
      "}").mkString("\n")

  /**
   * Add additional relations that are implied by the structure
   */
  def modify() = {
    var newRelations = Set[DepRel[E, W]]()

    //
    // Copy dependencies from parent to child of a "conj_and" relationship 
    //
    newRelations ++= {
      val childMap = groupRelations(relations ++ newRelations)
      (for (
        (_, children) <- childMap;
        (Conj(rel), depNode @ DepNode(_, _, dTag, _, _)) <- children if rel.value == "conj_and"
      ) yield {
        children.filterNot(_._2 == depNode).map { case (r, n) => DepRel(r, depNode.asInstanceOf[DepNode[E]], n) }
      }).flatten
    }

    //
    // From "He was found murdered", extract "He was murdered"
    //
    newRelations ++= {
      val childMap = groupRelations(relations ++ newRelations)
      (for (
        (_, children) <- childMap if children.exists(_._1.isInstanceOf[NSubjPass]);
        (Dep(rel), depNode @ DepNode(_, _, dTag, _, _)) <- children if dTag.startsWith("VB")
      ) yield {
        val dChildren = childMap.get(depNode.asInstanceOf[DepNode[E]])
        if (dChildren == None || !dChildren.get.exists(_._1.isInstanceOf[NSubj]))
          children.filterNot(_._2 == depNode).map { case (r, n) => DepRel(r, depNode.asInstanceOf[DepNode[E]], n) }
        else
          Seq()
      }).flatten
    }

    DepGraph(relations ++ newRelations, sourceTree, source)
  }
}
