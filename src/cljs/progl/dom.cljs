(ns progl.dom
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [c2.dom :as dom]
            [c2.event :refer [on]]
            [clojure.string :as s]
            [goog.events :as events]
            [cljs.core.async :as async :refer [map< >!]]))

(defn set-value! [node value]
  (set! (.-value node) value))

(defn set-innerhtml! [node html]
  (set! (.-innerHTML node) html))

(defn element-by-id [id]
  (.getElementById js/document (name id)))

(defn element-by-class [el & class-names]
  (.querySelector el (s/join (map #(str "." (name %)) class-names))))

(defn query [el selector]
  (.querySelectorAll el selector))

(defn elements-by-class [el & class-names]
  (.querySelectorAll el (s/join (map #(str "." (name %)) class-names))))

(defn class-pattern [class-name]
  (re-pattern (str "(^| )" (name class-name) "($| )")))

(defn has-class? [node class-name]
  (not (nil? (re-find (class-pattern class-name) (dom/attr node :class)))))

(defn add-class! [node class-name]
  (when-not (has-class? node class-name)
    (dom/attr node :class (s/trim (str (dom/attr node :class) " " (name class-name))))))

(defn remove-class! [node class-name]
  (when (has-class? node class-name)
    (dom/attr node :class (s/trim (s/replace (dom/attr node :class) (class-pattern class-name) " ")))))

(defn toggle-class! [node class-name]
  (if (has-class? node class-name)
    (remove-class! node class-name)
    (add-class! node class-name)))

(defn remove-classes! [class-name]
  (doseq [el (elements-by-class js/document class-name)]
    (remove-class! el class-name)))

(defn on-click [node f]
  (set! (.-onclick node) f))

(defn on-window-load [f]
  (set! (.-onload js/window) f))

(defn on-input [node f]
  (set! (.-oninput node) f))

(defn listen [el evt-type]
  (let [out (async/chan)]
    (events/listen el evt-type
                   (fn [evt] (async/put! out evt)))
    out))

(defn target-value [evt]
  (-> evt .-target .-value))

(defn listen-value [el evt-type]
  (async/map< target-value (listen el evt-type)))

(defn c2-listen [id evt-type]
  (let [out (async/chan)]
    (on id evt-type (fn [data node evt]
                      (go (>! out [data node evt]))))
    out))
