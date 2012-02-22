(ns org.bandm.android.SoundActivity
  (:gen-class :extends android.app.Activity
              :main false
              :exposes-methods {onCreate superOnCreate})
  (:import [android.view KeyEvent View$OnClickListener]
           [android.webkit WebSettings$ZoomDensity WebViewClient]
           [org.bandm.android R$id R$layout]
           [android.util Log]

           [com.jjoe64.graphview BarGraphView]
           [com.jjoe64.graphview GraphView GraphView$GraphViewData GraphView$GraphViewSeries]
           ))

(defn startRecording [startRecBtn stopRecBtn]
  (.setEnabled startRecBtn false)
  (.setEnabled stopRecBtn true)
  (.requestFocus stopRecBtn)
  )

(defn stopRecording [startRecBtn stopRecBtn]
  (.setEnabled startRecBtn true)
  (.setEnabled stopRecBtn false)
  (.requestFocus startRecBtn)
  )

(defn clearHandler [textField]
  (let [
        randNum (java.lang.Math/ceil(* (java.lang.Math/random) 100))
        ]
      (.setText textField (apply str ["rand: " randNum]))
      ))

(defn -onCreate
  "Called when the activity is initialised."
  [this bundle]
  (doto this
    (.superOnCreate bundle)
    (.setContentView R$layout/main)
    )
  (let 
    [
     valRevolvingSpeed (.findViewById this R$id/valRevolvingSpeed)
     startRecBtn (.findViewById this R$id/start)
     stopRecBtn (.findViewById this R$id/stop)
     clearBtn (.findViewById this R$id/clear)
     ]

    (.setEnabled stopRecBtn false)
    (.setEnabled startRecBtn true)

    (.setOnClickListener clearBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (clearHandler valRevolvingSpeed)
                 )))

    (.setOnClickListener startRecBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (startRecording startRecBtn stopRecBtn)
                 )))

    (.setOnClickListener stopRecBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (stopRecording startRecBtn stopRecBtn)
                 )))

    (Log/d (str *ns*) "graphview {")

    (let
    [
     layout (.findViewById this R$id/layout)
     arrGraphViewData (into-array [
                               (GraphView$GraphViewData. 1 0.4)
                               (GraphView$GraphViewData. 2 0.8)
                               (GraphView$GraphViewData. 3 1.1)
                               (GraphView$GraphViewData. 4 1.4)
                               (GraphView$GraphViewData. 5 1.6)
                               (GraphView$GraphViewData. 6 1.8)
                               (GraphView$GraphViewData. 7 1.9)
                               (GraphView$GraphViewData. 8 2.0)
                               (GraphView$GraphViewData. 9 2.05)
                               (GraphView$GraphViewData. 10 2.1)
                               (GraphView$GraphViewData. 11 2.12)
                               (GraphView$GraphViewData. 12 2.13)])

     graphView (BarGraphView. this "")    ; this: context; "": heading (the title)
     exampleSeries (GraphView$GraphViewSeries. arrGraphViewData)
     ]
     (.addSeries graphView exampleSeries)
     (.addView layout graphView)
    )

    ;; TODO (str *ns*) produces 'closure.core' instead of 'org.bandm.android.SoundActivity' hmm :(
    (Log/d (str *ns*) "graphview }")

    ))
