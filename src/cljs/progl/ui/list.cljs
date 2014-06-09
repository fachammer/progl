(ns progl.ui.list
  (:require-macros [c2.util :refer [bind!]])
  (:require [c2.event :refer [on]]
            [c2.core :refer [unify]]
            [c2.dom :refer [attr]]
            [progl.ui.search :refer [search-exact]]
            [progl.languages :as l]
            [progl.dom :as dom]))

(defn creators-label [creators]
  (str "Creator" (when-not (= 1 (count creators)) "s") ": "))

(defn creators-entry [creators]
  (when-not (empty? creators) [:tr [:td (creators-label creators)] [:td (apply str (interpose ", " creators))]]))

(defn table-entry [label value]
  [:tr [:td label] [:td value]])

(defn table-entry-when-not-empty [label value]
  (when-not (empty? value) (table-entry label value)))

(defn year-entry [year]
  (table-entry "Appearance year: " year))

(defn lang-link [lang-key lang-name]
  [:a {:class (name lang-key) :langk (name lang-key) :href "#"} lang-name])

(defn influences-entry [influences]
  (table-entry-when-not-empty "Influenced by: " (interpose ", " (map #(->> (% l/languages) :name (lang-link %)) influences))))

(defn influenced-entry [influenced]
  (table-entry-when-not-empty "Influenced: " (interpose ", " (map #(->> (% l/languages) :name (lang-link %)) influenced))))

(defn lang-list-item [[langk {lang-name :name
                              creators :creators
                              year :appearance-year
                              influences :influenced-by
                              influenced :influenced}]]
  [:li {:class (name langk)}
   [:div lang-name
    [:table {:id (str "table-" (name langk)) :class (str "hidden " (name langk))}
     (year-entry year)
     (creators-entry creators)
     (influences-entry influences)
     (influenced-entry influenced)]]])

(defn on-lang-list-item-click [langs]
  (fn [[langk# {lang-name# :name}] node# evt#]
    (if (= "A" (-> evt# .-target .-tagName))
      (search-exact langs (-> evt# .-target .-innerHTML))
      (do
        ;(search-exact langs lang-name#)
        (dom/toggle-class! (dom/element-by-id (str "table-" (name langk#))) :hidden)))))

(defn link? [node]
  (= "A" (-> node .-tagName)))

(defn on-lang-list-item-mouseover [[langk _] _ evt]
  (let [event-target (-> evt .-target)]
    (when (link? event-target)
      (l/highlight-lang! (attr event-target :langk)))
      (l/highlight-lang! langk)))

(defn on-lang-list-item-mouseout [[langk _] _ evt]
  (let [event-target (-> evt .-target)]
    (when (link? event-target)
      (l/dehighlight-lang! (attr event-target :langk)))
      (l/dehighlight-lang! langk)))

(defn language-list [id langs]
  (let [css-id (str "#" (name id))]
    (bind! css-id (unify @langs lang-list-item))
    (on css-id :click (on-lang-list-item-click @langs))
    (on css-id :mouseover on-lang-list-item-mouseover)
    (on css-id :mouseout on-lang-list-item-mouseout)))
