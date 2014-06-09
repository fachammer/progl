(ns progl.ui.search
  (:require [progl.dom :as dom]
            [progl.query :as q]
            [progl.languages :as l]))

(defn matches-text [n]
  (str (if (= n 0) "no" n) " language" (when (> n 1) "s") " selected"))

(defn show-langs [langs]
  (doseq [[langk _] langs]
    (dom/remove-class! (dom/element-by-id (str "table-" (name langk))) :hidden)))

(defn search [langs query]
  (-> (dom/element-by-id :search) (dom/set-value! query))
  (let [result (q/query langs query)]
    (swap! l/list-languages (fn [_] (if (empty? result) l/languages result)))
    (show-langs result)
    (l/remove-highlights!)
    (l/activate-langs! result)
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
