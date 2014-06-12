package plugin


trait Plugin2 {

  val name: String

  def whoAreYou() {
    println(s"I am $name.")
  }

}