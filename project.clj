(defproject progl "0.1.0-SNAPSHOT"
  :description "interactive infographic of programming languages"
  :url "http://github.com/fablman/progl"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [com.keminglabs/c2 "0.2.3"]
                 [enlive "1.1.5"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.2.2"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-ring "0.8.10"]]
  :uberjar-name "progl-standalone.jar"
  :ring {:handler progl.app/start}
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:output-to "public/js/cljs.js"
                   :pretty-print true
                   }
        :jar true}}})
