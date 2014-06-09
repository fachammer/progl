(ns progl.ui.core
  (:require [progl.dom :as dom]
            [c2.dom :refer [attr]]))

(defn remove-active! []
  (dom/remove-classes! :active))

(defn activate-lang! [lang-key]
  (doseq [el (dom/elements-by-class lang-key)]
    (dom/add-class! el :active)))

(defn activate-langs! [lang-keys]
  (doseq [k lang-keys]
    (activate-lang! k)))

(defn activate-list-langs! [lang-keys]
  (.log js/console lang-keys)
  (doseq [k lang-keys]
    (doseq [el (dom/elements-by-class k)]
      (let [tag (.-tagName el)]
        (when (or (= "A" tag)
                  (= "LI" tag))
          (dom/add-class! el :active))))))

(defn highlight-lang! [lang-key]
  (doseq [el (dom/elements-by-class lang-key)]
    (dom/add-class! el :highlight)
    (when (.-onhighlight el)
      (.onhighlight el el))))

(defn dehighlight-element! [element]
  (dom/remove-class! element :highlight)
  (when (.-ondehighlight element)
    (.ondehighlight element element)))

(defn dehighlight-lang! [lang-key]
  (doseq [el (dom/elements-by-class lang-key)]
    (dehighlight-element! el)))

(defn remove-highlights! []
  (doseq [el (dom/elements-by-class :highlight)]
    (dehighlight-element! el)))
