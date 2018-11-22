(ns google-maps-api-demo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [hickory.core :as h]
            [clojure.pprint :as pprint]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]))

(enable-console-print!)

(defonce app-state (r/atom {:origin ""
                            :destination ""}))


;;These contain JS objects that are initialized when the page is loaded
(def directions-service-atom (atom nil))
(def directions-display-atom (atom nil))

(defn titleize
  [str-or-kw]
  (string/capitalize (name str-or-kw)))

(defn text-box-on-change-fn
  [field]
  (fn [new-val]
    ;;when the user first gets an error upon submitting, the error should go away when they edit the
    ;;input field to correct the error.
    (if (:input-error? @app-state)
      (swap! app-state assoc :input-error? false))
    (swap! app-state assoc field (-> new-val
                                 .-target
                                 .-value))))

(defn text-box
  [field]
  [:div.form-group
   [:label {:for field} (titleize field)]
   [:input.form-control {:id field :type "text" :value (field @app-state)
                                 :on-change (text-box-on-change-fn field)}]])

(defn extract-instructions
  [response]
  (let [steps (-> response
                  (js->clj :keywordize-keys true)
                  :routes
                  first
                  :legs
                  first
                  :steps)]
    (mapv :instructions steps)))

(defn response-handler
  [resp status]
  (if (= status "NOT_FOUND")
    (swap! app-state assoc :input-error? true)
    (let [instructions (extract-instructions resp)
          directions-display @directions-display-atom]
      (.setDirections directions-display resp)
      (swap! app-state assoc :instructions instructions))))

(defn make-directions-req
  []
  (let [{:keys [origin destination]} @app-state
        directions-service @directions-service-atom]
    (.route directions-service (clj->js {:origin origin
                                         :destination destination
                                         :travelMode "DRIVING"})
            response-handler)))

(defn location-form
  []
  [:form.col-sm-6 {:on-submit (fn [event]
                                ;;so the page doesn't refresh when the user submits
                                (do
                                  (.preventDefault event)
                                  (make-directions-req)))}
   [text-box :origin]
   [text-box :destination]
   [:div.row (when (:input-error? @app-state)
       [:div.alert.alert-danger "Route not found! Check your input!"])]
   [:div.row [:input.btn.btn-primary {:type :submit :value "Submit"}]]])

(defn init-map
  []
  (let [directions-service (google.maps.DirectionsService.)
        directions-display (google.maps.DirectionsRenderer.)
        map (google.maps.Map. (.getElementById js/document "map")
                 (clj->js {:zoom 7
                           :center {:lat 33.74 :lng -84.38}}))]
    (.setMap directions-display map)
    (reset! directions-service-atom directions-service)
    (reset! directions-display-atom directions-display)))

(defn map-elm
  []
  (r/create-class
   {:reagent-render (fn [] [:div#map])
    :component-did-mount (fn [this] (init-map))}))

(defn directions-table
  []
  [:table.table
   (into [:tbody]
         ;;Reagent requires everything to be as hiccup before it renders it.
         ;;since the instructions provided by the directions API have html tags in them, we have to use
         ;;reagent's dangerouslySetInnerHTML.
         ;;If we just try to include the html instruction as the text for this div, the <b> tags won't be parsed
         ;;and will show in the table
         (mapv (fn [instruction] [:tr [:td [:div {:dangerouslySetInnerHTML {:__html instruction}}]]])
          (:instructions @app-state)))])

(defn page
  []
  [:div.container
   [:h2 "Google Maps API CLJS Example App"]
   [:div.row [location-form]]
   [:div.row
    [:div.col-sm-6
     [:h4 "Map"]
     [map-elm]]
    (when (:instructions @app-state)
      [:div.col-sm-6
       [:h4 "Direction Steps"]
       [directions-table]])]])

(defn on-js-reload []
  ;; optionally touch your app-app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-app-state update-in [:__figwheel_counter] inc)
  (r/render [page] (.getElementById js/document "app")))
