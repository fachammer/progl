(ns progl.ui.select
  (:require [cljs.core.async :refer [chan mult tap map<]]
            [progl.query :refer [query]]
            [progl.languages :refer [languages]]
            [progl.util :refer [tap-new]]))

(def in (chan))
(def in-take (mult in))


(def out (mult (map< #(query languages %) (tap-new in-take))))
