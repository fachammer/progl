(ns progl.dom
  (:require [c2.dom :as dom]
            [clojure.string :as s]))

(defn set-value! [node value]
  (set! (.-value node) value))

(defn set-innerhtml! [node html]
  (set! (.-innerHTML node) html))

(defn element-by-id [id]
  (.getElementById js/document (name id)))

(defn elements-by-class [class-name]
  (.querySelectorAll js/document (str "." (name class-name))))

(defn class-pattern [class-name]
  (re-pattern (str "(^| )" (name class-name) "($| )")))

(defn has-class? [node class-name]
  (not (nil? (re-find (class-pattern class-name) (dom/attr node :class)))))

(defn add-class! [node class-name]
  (when-not (has-class? node class-name)
    (dom/attr node :class (s/trim (str (dom/attr node :class) " " (name class-name))))))

(defn remove-class! [node class-name]
  (when (has-class? node class-name)
    (dom/attr node :class (s/replace (dom/attr node :class) (class-pattern class-name) ""))))

(defn toggle-class! [node class-name]
  (if (has-class? node class-name)
    (remove-class! node class-name)
    (add-class! node class-name)))

(defn remove-classes! [class-name]
  (doseq [el (elements-by-class class-name)]
    (remove-class! el class-name)))

(defn on-click [node f]
  (set! (.-onclick node) f))

(defn on-window-load [f]
  (set! (.-onload js/window) f))

(defn on-input [node f]
  (set! (.-oninput node) f))
