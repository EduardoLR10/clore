(ns clore.renderer
  (:require
    [clore.datomic :as datomic]
    [datomic.api :as d])
  (:import (java.text SimpleDateFormat)))

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
    [:div {:class "reminder-component"}
     [:div {:style {:display "inline"}}
      [:label {:class "reminder-title" :for "title"} "Reminder Title: "]
      [:input {:type "text" :name "title" :required ""}]]
     [:div {:style {:display "inline"}}
      [:label {:class "reminder-until" :for "until"} "Until: "]
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
    [:h1 [:a {:style {:color "#90b4fe"} :href "/"} "Clore"]])
   (list [:br] [:br])
   reminder-buttons
   (list [:br] [:br] [:br] [:br])
   (list
    [:div
     (add-reminder-component path)])
   (list [:br] [:br] [:br] [:br])))

(defn ^:private render-date [date]
  (let [df (SimpleDateFormat/new "EEE MMM d HH:mm:ss zzz yyyy")]
    (.format (java.text.SimpleDateFormat. "dd/MM/yyyy") (.parse df (str date)))))

(defn ^:private render-reminder
  [path {:reminder/keys [id title created-at until]}]
  [:tr {:class "space-under"}
   [:td [:b {:class "registered-title"} title]]
   [:td [:b {:class "registered-date"} (render-date created-at)]]
   [:td [:b {:class "registered-date"} (render-date until)]]
   [:td
    [:div {:style {:display "flex" :align-items "center" :justify-content "center"}}
     [:form {:method "post" :action (str "/delete-reminder/" id)}
      [:input {:type "hidden" :name "path" :required "" :value path}]
      [:button {:class "delete-button"} "Delete"]]]]])

(defn render-reminders
  [path db query current-date]
  (let [items (map (partial render-reminder path) (d/q query db current-date))]
    (prn items)
    (if (seq items)
      (list
       [:br]
       [:br]
       [:br]       
       [:table 
        [:tr {:class "space-under"}
         [:th [:b {:class "registered-title-label"} "Reminder"]]
         [:th [:b {:class "registered-date-label"} "Created"]]
         [:th [:b {:class "registered-date-label"} "Until"]]
         [:th]]
        items])
      '())))

(defn short-term-reminders
  [path db current-date]
  (render-reminders path db datomic/short-term-query current-date))

(defn long-term-reminders
  [path db current-date]
  (render-reminders path db datomic/long-term-query current-date))

(defn term-reminders
  [path db current-date]
  (render-reminders path db datomic/term-query current-date))

(defn title [text]
  [:h2 {:style {:color "#90b4fe" :text-align "center"}} text])
