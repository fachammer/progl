(ns progl.util
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]])
  (:require [cljs.core.async :as async :refer [chan <! >! mult tap pipe map< alts! timeout take! put! close!]]))

(defn on-channel [ch f]
  (m/go-loop []
             (f (<! ch))
             (recur)))

(defn with-quotes [s]
  (str "\"" s "\""))

(defn link? [node]
  (= "A" (-> node .-tagName)))

(defn pipe-map< [src f target]
  (pipe (map< f src) target))

(defn tap-new [ch]
  (tap ch (chan)))

(defn throttle [ch delay-time]
  (let [out (chan)]
    (m/go-loop [cur nil]
               (alt!
                  ch ([v] (if v (recur v) (close! out)))
                  (timeout delay-time) (when-not (nil? cur) (>! out cur)))
               (recur cur))
    out))
