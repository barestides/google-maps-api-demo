(ns google-maps-api-demo.core
  (:require [reagent.core :as r]
            [clojure.string :as string]))

(enable-console-print!)

(defonce app-state (r/atom {:origin ""
                            :destination ""}))

;;These contain JS objects that are initialized when the page is loaded
(def directions-service-atom (atom nil))
(def directions-display-atom (atom nil))

;;;;;;;;;;;;;
;;; Utils ;;;
;;;;;;;;;;;;;

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

(defn extract-instructions
  [legs]
  (mapv :instructions (:steps legs)))

(defn titleize
  [str-or-kw]
  (string/capitalize (name str-or-kw)))

;;;;;;;;;;;;;
;;; State ;;;
;;;;;;;;;;;;;

(defn response-handler
  "Takes the response and status from the instructions request.
  On failure, sets the `input-error?` for validation.
  On success, pulls the instructions and the formatted origin and
  destination out of the resopnse into the app-state."
  [resp status]
  (if (= status "NOT_FOUND")
    (swap! app-state assoc :input-error? true)
    (let [directions-display @directions-display-atom
          legs (-> resp
                  (js->clj :keywordize-keys true)
                  :routes
                  first
                  :legs
                  first)
          {:keys [start_address end_address]} legs
          instructions (extract-instructions legs)]
      (.setDirections directions-display resp)
      (swap! app-state assoc :instructions instructions
             ;;The strings provided by the `start_address` and `end_address`
             ;;from the response are nicely capitalized and formatted.
             ;;We use this to show what directions are currently being displayed
             ;;in the map / directions table.
             :current-origin start_address :current-destination end_address))))

(defn make-directions-req!
  []
  (let [{:keys [origin destination]} @app-state
        directions-service @directions-service-atom]
    (.route directions-service (clj->js {:origin origin
                                         :destination destination
                                         :travelMode "DRIVING"})
            response-handler)))

(defn init-map!
  []
  (let [directions-service (google.maps.DirectionsService.)
        directions-display (google.maps.DirectionsRenderer.)
        map (google.maps.Map. (.getElementById js/document "map")
                 (clj->js {:zoom 7
                           :center {:lat 33.74 :lng -84.38}}))]
    (.setMap directions-display map)
    (reset! directions-service-atom directions-service)
    (reset! directions-display-atom directions-display)))

;;;;;;;;;;;;;
;;; Views ;;;
;;;;;;;;;;;;;

(defn text-box
  [field]
  [:div.form-group
   [:label {:for field} (titleize field)]
   [:input.form-control {:id field :type "text" :value (field @app-state)
                         :on-change (text-box-on-change-fn field)}]])

(defn location-form
  []
  [:form.col-sm-6 {:on-submit (fn [event]
                                (do
                                  ;;so the page doesn't refresh when the user submits
                                  (.preventDefault event)
                                  (make-directions-req!)))}
   [text-box :origin]
   [text-box :destination]
   [:div.col-sm-12
    [:div.row (when (:input-error? @app-state)
                [:div.alert.alert-danger "Route not found! Check your input!"])]
    [:div.row [:input.btn.btn-primary {:type :submit :value "Submit"}]]]])

(defn map-elm
  []
  (r/create-class
   {:reagent-render (fn [] [:div#map])
    ;;The awkward reality of reagent...
    ;;We need to supply an HTML element that exists on the page to
    ;;the google.maps.Map constructor, but the #map div won't exist
    ;;until after this component is rendered.
    ;;Reagent supplies this concept of classes with lifecycle methods
    ;;to handle these situations:
    ;;https://github.com/reagent-project/reagent/blob/master/doc/CreatingReagentComponents.md#form-3-a-class-with-life-cycle-methods
    :component-did-mount (fn [this] (init-map!))}))

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
  [:div#page.container
   [:h2 "Google Maps API CLJS Example App"]
   [:div.row [location-form]]
   [:div#directions-output.row
    [:div.col-sm-6
     [:h4 "Map"]
     [map-elm]]
    (when (:instructions @app-state)
      [:div.col-sm-6
       (let [{:keys [current-origin current-destination]} @app-state]
         [:h4 (str "Directions from " current-origin " to " current-destination)])
       [directions-table]])]])

(defn on-js-reload []
  (r/render [page] (.getElementById js/document "app")))

(on-js-reload)
