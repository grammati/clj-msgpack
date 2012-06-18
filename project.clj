(defproject clj-msgpack "0.2.0"
  :description "Messagepack serialization library for Clojure."
  :dependencies [
    [org.clojure/clojure "1.4.0"]
    [org.msgpack/msgpack "0.6.6" :exclusions [junit]]
    ]
  :repositories {"msgpack" "http://msgpack.org/maven2/"}
  :dev-dependnecies [[junit/junit "4.8.2"]]
  )
