(ns progl.ui.search
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]])
  (:require [progl.dom :as dom]
            [progl.query :as q]
            [progl.util :as util :refer [on-channel]]
            [progl.languages :as l]
            [progl.ui.select :as select]
            [cljs.core.async :as async :refer [pipe]]))

(defn set-search-input [text]
  (-> (dom/element-by-id :search) (dom/set-value! text)))

(defn matches-text [n]
  (str (if (= n 0) "no" n) " language" (when (> n 1) "s") " found"))

(defn set-matches-text [matches]
  (-> (dom/element-by-id :matches) (dom/set-innerhtml! (matches-text (count matches)))))

(defn language-search [search-id langs]
  (pipe (dom/listen-value (dom/element-by-id :search) "input") select/in)
  (on-channel select/in-take set-search-input)
  (on-channel select/out set-matches-text))
