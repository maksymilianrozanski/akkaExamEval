package exams.http

import scalacss.DevDefaults._

object QuestionStyles extends StyleSheet.Inline {

  import dsl._

  val questionContainer: StyleA = style(
    padding(10 px),
    boxShadow := "0 4px 8px 0 rgba(0,0,0,0.2)"
  )

  val imageStyle: StyleA = style(
    maxWidth(100 %%),
    padding(10 px),
  )
}
