(ns progl.core
  (:require [progl.ui.search :refer [language-search search]]
            [progl.ui.graph :refer [language-graph]]
            [progl.ui.list :refer [language-list]]
            [progl.languages :as l]
            [progl.dom :as dom]))

(defn on-window-load-handler []
  (language-search :search l/languages)
  (language-graph :graph l/languages)
  (language-list :list l/languages)
  (search l/languages ""))

(dom/on-window-load on-window-load-handler)
