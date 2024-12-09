(ns clojure-demo.renderer
  (:require
    [clojure-demo.datomic :as datomic]
    [datomic.api :as d])
  (:import (java.text SimpleDateFormat)))

;; <!-- HTML !-->
;; <button class="button-82-pushable" role="button">
;;   <span class="button-82-shadow"></span>
;;   <span class="button-82-edge"></span>
;;   <span class="button-82-front text">
;;     Button 82
;;   </span>
;; </button>

(defn ^:private reminder-selector
  [text]
  [:button {:class "button-82-pushable" :role "button"}
   [:span {:class "button-82-shadow"}]
   [:span {:class "button-82-edge"}]
   [:span {:class "button-82-front text"}
    text]])

(def reminder-buttons
  (list
   [:div
    {:class "selectors"}
    [:form {:action "/short-term"}
     (reminder-selector "Short Term")]
    [:form {:action "/long-term"}
     (reminder-selector "Long Term")]]))

(defn ^:private add-reminder-component
  [path]
  (list
   [:form {:method "post" :action "/add-reminder"}
    [:div {:style {:display "inline"}}
     [:div {:style {:display "inline"}}
      [:label {:for "title"} "Reminder Title:"]
      [:input {:type "text" :name "title" :required ""}]]
     [:div {:style {:display "inline"}}
      [:label {:for "until"} "Until then:"]
      [:input {:type "date" :name "until" :required ""}]]
     [:div {:style {:display "inline"}}
      [:input {:type "hidden" :name "path" :required "" :value path}]]
     [:div {:style {:display "inline"}}
      [:input {:type "submit" :value "Create"}]]]]))

(defn common-header
  [path]
  (concat
   (list
    [:head
    [:link {:rel "stylesheet" :href "reset.css"}]
    [:link {:rel "stylesheet" :href "main.css"}]]
    [:h1 [:a {:href "/"} "Clojure Demonstration"]])
   (list [:br] [:br])
   reminder-buttons
   (list [:br] [:br])
   (list
    [:div
     (add-reminder-component path)])))

(defn ^:private render-date [date]
  (let [df (SimpleDateFormat/new "EEE MMM d HH:mm:ss zzz yyyy")]
    (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (.parse df (str date)))))

(defn ^:private render-reminder
  [path {:reminder/keys [id title created-at until]}]
  [:li
   [:div {:style {:display "inline"}}
    [:b {:style {:font-weight "bold"}} title] " "
    (render-date created-at) " "
    (render-date until) "   "
    [:div {:style {:display "inline-block"}}
    [:form {:method "post" :action (str "/delete-reminder/" id)}
     [:input {:type "hidden" :name "path" :required "" :value path}]
     [:button "Delete"]]]]])

(defn render-reminders
  [path db query current-date]
  (prn (d/q query db current-date))
  (list
   [:ul
   (map (partial render-reminder path) (d/q query db current-date))]))

(defn short-term-reminders
  [path db current-date]
  (render-reminders path db datomic/short-term-query current-date))

(defn long-term-reminders
  [path db current-date]
  (render-reminders path db datomic/long-term-query current-date))

(defn title [text]
  [:h2 {:style {:text-align "center"}} text])
