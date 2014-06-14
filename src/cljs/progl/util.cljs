(ns progl.util
  (:require-macros [cljs.core.async.macros :as m])
  (:require [cljs.core.async :as async :refer [chan <! mult tap]]))

(defn on-channel [ch f]
  (let [channel (tap ch (chan))]
    (m/go-loop []
               (f (<! channel))
               (recur))))
