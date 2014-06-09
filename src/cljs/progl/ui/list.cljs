(ns progl.ui.list
  (:require-macros [c2.util :refer [bind!]])
  (:require [c2.event :refer [on]]
            [c2.core :refer [unify]]
            [c2.dom :refer [attr]]
            [progl.ui.search :refer [search-exact]]
            [progl.ui.core :as ui]
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

(defn influences-entry [influences languages]
  (table-entry-when-not-empty "Influenced by: " (interpose ", " (map #(->> (% languages) :name (lang-link %)) influences))))

(defn influenced-entry [influenced languages]
  (table-entry-when-not-empty "Influenced: " (interpose ", " (map #(->> (% languages) :name (lang-link %)) influenced))))

(defn lang-list-item [[[langk {lang-name :name
                              creators :creators
                              year :appearance-year
                              influences :influenced-by
                              influenced :influenced}] languages]]
  [:li {:class (str "active " (name langk))}
   [:div lang-name
    [:table {:class (name langk)}
     (year-entry year)
     (creators-entry creators)
     (influences-entry influences languages)
     (influenced-entry influenced languages)]]])

(defn link? [node]
  (= "A" (-> node .-tagName)))

(defn on-lang-list-item-click [langs]
  (fn [_ _ evt#]
    (when (link? (.-target evt#))
      (search-exact langs (-> evt# .-target .-innerHTML)))))

(defn on-lang-list-item-mouseover [[[langk _] _] _ evt]
  (let [event-target (-> evt .-target)]
    (when (link? event-target)
      (ui/highlight-lang! (attr event-target :langk)))
    (ui/highlight-lang! langk)))

(defn on-lang-list-item-mouseout [_ _ _]
  (ui/remove-highlights!))

(defn language-list [id langs]
  (let [css-id (str "#" (name id))]
    (bind! css-id (unify (map #(vector % langs) langs) lang-list-item))
    (on css-id :click (on-lang-list-item-click langs))
    (on css-id :mouseover on-lang-list-item-mouseover)
    (on css-id :mouseout on-lang-list-item-mouseout)))
