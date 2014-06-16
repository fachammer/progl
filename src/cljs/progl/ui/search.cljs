(ns progl.ui.search
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]])
  (:require [progl.dom :as dom]
            [progl.query :as q]
            [progl.util :as util :refer [on-channel tap-new throttle]]
            [progl.languages :as l]
            [progl.ui.select :as select]
            [cljs.core.async :as async :refer [pipe]]))

(defn set-search-input [text]
  (-> (dom/element-by-id :search) (dom/set-value! text)))

(defn matches-text [n]
  (str (if (= n 0) "no" n) " language" (when (> n 1) "s") " found"))

(defn set-matches-text [matches]
  (-> (dom/element-by-id :matches) (dom/set-innerhtml! (matches-text (count matches)))))

(def input-delay 350)

(defn language-search [search-id langs]
  (pipe (throttle (dom/listen-value (dom/element-by-id :search) "input") input-delay) select/in)
  (on-channel (tap-new select/in-take) set-search-input)
  (on-channel (tap-new select/out) set-matches-text))
