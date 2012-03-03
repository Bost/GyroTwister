(ns org.bandm.android.SoundActivity
  (:gen-class :extends android.app.Activity
              :main false
              :exposes-methods {onCreate superOnCreate})
  (:import [android.view KeyEvent View$OnClickListener]
           [android.webkit WebSettings$ZoomDensity WebViewClient]
           [org.bandm.android R$id R$layout]
           [android.util Log]
           [android.os Environment Bundle]

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

(defn isUSBCableConnected []
  (let [extStorState (.getExternalStorageState Environment)]
    (not (= extStorState Environment/MEDIA_MOUNTED))))

(defn clearHandler [textField]
  (let [
        randNum (java.lang.Math/ceil(* (java.lang.Math/random) 100))
        ]
      (.setText textField (apply str ["" randNum]))
      ))

(defn logAction [logFunc msg]
    (map #(logFunc %1 %2) [(str *ns*)] [msg]))

;the full fn form lets you use destructuring in the argument vector to unpack the incoming sequence
;(map (fn [[s e]] (java.net.URLEncoder/encode s e)) [["bar & baz", "UTF-8"]])

(defn -onCreate
  "Called when the activity is initialised."
  [this bundle]
  (doto this
    (Log/d (str *ns*) "logAction -onCreate {")
    (.superOnCreate bundle)
    (.setContentView R$layout/main)
    )
;		boolean isExtStorageRW = !isUSBCableConnected();
;		String logLevel = isExtStorageRW ? debug : warn;
;		String msg = "External storage writable: " + isExtStorageRW;
;		logAction(logLevel, msg);

  ;(apply #(Log/d) ["tagname" "test debug level"])
  ;(logAction Log/i "test info level")
    (Log/d (str *ns*) "logAction -onCreate }")

  (let
    [
;     valRevolvingSpeed (.findViewById this R$id/valRevolvingSpeed)
;     startRecBtn (.findViewById this R$id/start)
;     stopRecBtn (.findViewById this R$id/stop)
;     clearBtn (.findViewById this R$id/clear)
      [valRevolvingSpeed startRecBtn stopRecBtn clearBtn]
      (map #(.findViewById this %)
           [R$id/valRevolvingSpeed R$id/start R$id/stop R$id/clear])
     ]

    (.setEnabled stopRecBtn false)
    (.setEnabled startRecBtn true)

    ; create a proxy object listener which implements the onClick method
    ; and attach listener to the clearBtn using the setOnClickListener method
    ; Use reify (= 'materialize') instead of proxy because:
    ; - reify can only implement interfaces. It cannot exted classes (proxy can)
    ; - reify is directly supported by the host platform
    ; search for "The Expression Problem Revisited"
    (let [
          listener (reify View$OnClickListener
                     (onClick [this view]
                              (clearHandler valRevolvingSpeed)
                              ))
          ]
      (.setOnClickListener clearBtn listener)
      )

    (let [
          listener (reify View$OnClickListener
                     (onClick [this view]
                              (startRecording startRecBtn stopRecBtn)
                              ))
          ]
      (.setOnClickListener startRecBtn listener)
      )

    (let [
          listener (reify View$OnClickListener
                     (onClick [this view]
                              (stopRecording startRecBtn stopRecBtn)
                              ))
          ]
      (.setOnClickListener stopRecBtn listener)
      )

    (Log/d (str *ns*) "graphview {")

    ;(let [foo (into [] (map #(GraphView$GraphViewData. %1 %2) [1 2] [0.4 0.3]))]
    ;  (Log/d (str *ns*) foo)
    ;)

    (let
    [
     layout (.findViewById this R$id/layout)
     arrGraphViewData (into-array (map #(GraphView$GraphViewData. %1 %2)
                      [  1   2   3   4   5   6   7   8    9  10   11   12]
                      [0.4 0.8 1.1 1.4 1.6 1.8 1.9 2.0 2.05 2.1 2.12 2.13]
                   ))

;    arrGraphViewData (into-array [(GraphView$GraphViewData. 1 0.4)
;                                   (GraphView$GraphViewData. 2 0.8)
;                                   ...
;                                   (GraphView$GraphViewData. 12 2.13)])

     graphView (BarGraphView. this "")    ; this: context; "": heading (the title)
     exampleSeries (GraphView$GraphViewSeries. arrGraphViewData)
     ]
     (.addSeries graphView exampleSeries)
     (.addView layout graphView)
     )

    ;; TODO (str *ns*) produces 'closure.core' instead of 'org.bandm.android.SoundActivity' hmm :(
    (Log/d (str *ns*) "graphview }")

    ))
