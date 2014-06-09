(ns progl.ui.search
  (:require [progl.dom :as dom]
            [progl.query :as q]
            [progl.ui.core :as ui]
            [progl.languages :as l]))

(defn matches-text [n]
  (str (if (= n 0) "no" n) " language" (when (> n 1) "s") " selected"))

(defn search [langs query]
  (.log js/console (str "Query: " query))
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

(defn search-exact [langs query]
  (search langs (str "\"" query "\"")))

(defn on-search-input [langs]
  (fn [evt#]
    (let [query (-> evt# .-target .-value)]
      (search langs query))))

(defn language-search [search-id langs]
  (-> (dom/element-by-id search-id)
      (dom/on-input (on-search-input langs))))
