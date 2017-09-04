package onvifsnapshottaker.db

case class Preset(name: String, displayName: String)

case class Presets(presets: List[Preset])
