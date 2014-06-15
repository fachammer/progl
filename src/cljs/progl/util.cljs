(ns progl.util
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :as async :refer [chan <! mult tap pipe map< alt! alts! timeout]]))

(defn on-channel [ch f]
  (let [channel (tap ch (chan))]
    (m/go-loop []
               (f (<! channel))
               (recur))))

(defn with-quotes [s]
  (str "\"" s "\""))

(defn link? [node]
  (= "A" (-> node .-tagName)))

(defn pipe-map< [src f target]
  (pipe (map< f src) target))
