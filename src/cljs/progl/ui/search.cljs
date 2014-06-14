(ns progl.ui.search
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]])
  (:require [progl.dom :as dom]
            [progl.query :as q]
            [progl.util :as util :refer [on-channel]]
            [progl.ui.core :as ui]
            [progl.languages :as l]
            [progl.ui.select :as select]
            [cljs.core.async :as async :refer [chan pipe tap]]))

(defn matches-text [n]
  (str (if (= n 0) "no" n) " language" (when (> n 1) "s") " found"))

(defn search [langs query]
  (-> (dom/element-by-id :search) (dom/set-value! query))
  (let [result (q/query langs query)]
    (ui/remove-highlights!)
    (ui/remove-active!)
    (if (= query "")
      (ui/activate-list-langs! (keys result))
      (ui/activate-langs! (keys result)))
    (-> (dom/element-by-id :matches)
        (dom/set-innerhtml! (matches-text (count result))))
    result))

(defn set-search-input [text]
  (-> (dom/element-by-id :search) (dom/set-value! text)))

(defn set-matches-text [matches]
  (-> (dom/element-by-id :matches) (dom/set-innerhtml! (matches-text (count matches)))))

(defn language-search [search-id langs]
  (pipe (dom/listen-value (dom/element-by-id :search) "input") select/in)
  (on-channel select/in-take set-search-input)
  (on-channel select/out set-matches-text))
