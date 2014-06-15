(ns progl.core
  (:require [progl.ui.search :refer [language-search]]
            [progl.ui.graph :refer [language-graph]]
            [progl.ui.list :refer [language-list]]
            [progl.ui.select :as select]
            [progl.languages :as l]
            [progl.dom :as dom]
            [cljs.core.async :refer [put!]]))

(defn on-window-load-handler []
  (language-search :search l/languages)
  (language-graph :graph l/languages)
  (language-list :list l/languages)
  (put! select/in ""))

(dom/on-window-load on-window-load-handler)
