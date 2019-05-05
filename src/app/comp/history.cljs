
(ns app.comp.history
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> list-> cursor-> span div]]
            [app.config :as config]
            [respo-ui.comp.icon :refer [comp-icon]]
            [respo-alerts.comp.alerts :refer [comp-prompt]]
            ["dayjs" :as dayjs]
            [inflow-popup.comp.dialog :refer [comp-menu-dialog]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(defcomp
 comp-done-task
 (states task)
 (let [state (or (:data states) {:show-menu? false})]
   (div
    {:style (merge
             ui/row
             {:padding "4px 16px"}
             (when (:show-menu? state) {:background-color (hsl 0 0 94)})),
     :on-click (fn [e d! m!] (m! (assoc state :show-menu? true)))}
    (div
     {:style {:min-width 72, :color (hsl 0 0 80), :font-size 12}}
     (<> (.format (dayjs (:finished-time task)) "MM-DD HH:mm")))
    (div {:style (merge ui/flex {:line-height "24px"})} (<> (:text task)))
    (when (:show-menu? state)
      (comp-menu-dialog
       (fn [result d! m!]
         (m! (assoc state :show-menu? false))
         (when (= :put-back result) (d! :task/put-back (:id task))))
       {:put-back (<> "Put back")})))))

(defcomp
 comp-history
 (states data finished-tasks)
 (let [year (:year data), week (:week data)]
   (div
    {:style (merge ui/flex {:padding "8px 8px", :overflow :auto})}
    (div
     {:style (merge ui/row-parted {:padding "0 8px", :color (hsl 0 0 80)})}
     (<>
      (<< "Histories of ~{week}th week in ~{year}")
      {:font-family ui/font-fancy, :font-size 20})
     (div
      {:style ui/row}
      (span
       {:inner-text "Prev",
        :style ui/link,
        :on-click (fn [e d! m!]
          (d!
           :router/change
           {:name :history,
            :data (if (<= week 1)
              {:year (dec year), :week 53}
              {:year year, :week (dec week)})}))})
      (=< 8 nil)
      (span
       {:inner-text "Next",
        :style ui/link,
        :on-click (fn [e d! m!]
          (d!
           :router/change
           {:name :history,
            :data (if (>= week 53)
              {:year (inc year), :week 1}
              {:year year, :week (inc week)})}))})))
    (if (empty? finished-tasks)
      (<> "No tasks.")
      (let [grouped-tasks (->> (vals finished-tasks)
                               (group-by
                                (fn [task]
                                  (.format (dayjs (:finished-time task)) "YYYY-MM-DD"))))]
        (list->
         {}
         (->> grouped-tasks
              (sort (fn [x y] (compare (first y) (first x))))
              (map
               (fn [[date-string task-list]]
                 [date-string
                  (div
                   {:style (merge ui/column {:margin-top 24})}
                   (let [the-day (dayjs date-string)]
                     (div
                      {:style ui/row-parted}
                      (span
                       {:style {:font-family ui/font-fancy, :padding "0 8px"}}
                       (<> (.format the-day "ddd"))
                       (=< 12 nil)
                       (<> (.format the-day "MM-DD")))))
                   (=< nil 4)
                   (list->
                    {}
                    (->> task-list
                         (sort-by (fn [task] (unchecked-negate (:finished-time task))))
                         (map
                          (fn [task]
                            [(:id task) (cursor-> (:id task) comp-done-task states task)])))))])))))))))
