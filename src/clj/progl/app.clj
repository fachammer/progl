(ns progl.app
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (route/files "/")
  (route/resources "/")
  (route/not-found "Not Found"))

(def start
  (handler/site app-routes))
