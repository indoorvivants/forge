package my.frontend

import com.raquo.laminar.api.L._

val app = div(
  h1("Hello, World!"),
  p("This is a simple Laminar application.")
)

@main def hello =
  renderOnDomContentLoaded(org.scalajs.dom.document.getElementById("root"), app)
