(ns progl.app
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]))

(defroutes app-routes
  (route/files "/")
  (route/resources "/")
  (route/not-found "Not Found"))

(def start
  (handler/site app-routes))

(defn -main [port]
  (jetty/run-jetty start {:port port}))
