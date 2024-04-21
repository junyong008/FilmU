## 🎞 FilmU

* 1인 프로젝트 [기획, 디자인, 프론트엔드, 백엔드]
* 기획 및 프로토타입 개발 중

### 개요
* 나의 변화를 한편의 영화로 만들어주는 자기개발 서비스

### 프로토타입 기능
* 머신러닝 손가락 인식을 통한 원격 타이머
  
  <img width="1073" alt="hand-landmarks" src="https://github.com/junyong008/FilmU/assets/69251013/b2dfc5fd-e198-4cd8-86e9-16578c66fc61">

  ```kotlin
  /*
    1. TIP과 WRIST 사이의 거리를 계산
    2. PIP과 WRIST 사이의 거리를 계산
    3. (TIP ~ WRIST) 거리가 (PIP ~ WRIST) 거리보다 멀어지면 손가락이 펴진것으로 인식
    4. 엄지의 경우,
       1) (THUMB_TIP ~ INDEX_FINGER_MCP) 거리가 (THUMB_TIP ~ PINKY_MCP) 거리보다 짧고
       2) THUMB_(TIP, IP, CMC)의 각도가 수평과 유사할 때
       손가락이 펴진것으로 인식
  */
  
  // 유클리드 거리를 이용하여 두 점 사이의 거리를 계산
  fun calculateDistance(p1: NormalizedLandmark, p2: NormalizedLandmark): Double {
      val xDiff = p1.x() - p2.x()
      val yDiff = p1.y() - p2.y()
      return sqrt((xDiff * xDiff + yDiff * yDiff).toDouble())
  }
  
  // 점 B를 중심으로 하여 AB 벡터와 CB 벡터의 사이각 구하기
  fun calculateAngle(A: NormalizedLandmark, B: NormalizedLandmark, C: NormalizedLandmark): Double {
      val ABx = B.x() - A.x()
      val ABy = B.y() - A.y()
      val CBx = B.x() - C.x()
      val CBy = B.y() - C.y()
      val angleAB = atan2(ABy, ABx)
      val angleCB = atan2(CBy, CBx)
      var angle = (angleAB - angleCB).toDouble()
      if (angle < 0) angle += 2 * PI
      return Math.toDegrees(angle)
  }
  ```
  ![hand1](https://github.com/junyong008/FilmU/assets/69251013/ab1ffb62-92db-45b4-882f-65f5738fe648)
  ![hand2](https://github.com/junyong008/FilmU/assets/69251013/3e73e14a-66b1-46c8-8b6a-1e4b1d3702b4)

* OpenCV 이미지 처리를 통한 윤곽선 가이드 생성
  ![openCV](https://github.com/junyong008/FilmU/assets/69251013/c127b1df-23f0-44ba-abe8-9e35b60959ea)

* 과거 사진과 히스토그램 유사도 분석을 통한 자동 촬영
  ```kotlin
  // 히스토그램 비교를 통한 이미지 유사도 측정
  fun calculateImageSimilarity(image1: Mat, image2: Mat): Double {
      val histSize = MatOfInt(256)
      val ranges = MatOfFloat(0f, 256f)
      val hist1 = Mat()
      val hist2 = Mat()
      val accumulate = false
      Imgproc.calcHist(listOf(image1), MatOfInt(0), Mat(), hist1, histSize, ranges, accumulate)
      Imgproc.calcHist(listOf(image2), MatOfInt(0), Mat(), hist2, histSize, ranges, accumulate)
      return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_BHATTACHARYYA)
  }
  ```
  ![hist](https://github.com/junyong008/FilmU/assets/69251013/991bb46e-d43c-4783-ab33-1e2cc7e11820)
  > 이전 이미지와 유사하면 프로그레스바가 차오른다

* 자연스러운 비율 전환 애니메이션
  ![ratio](https://github.com/junyong008/FilmU/assets/69251013/d4fe5977-a3bd-414e-8dde-5a95057c2a7e)


### 로고 디자인 및 기획
<img width="1072" alt="Illustrator_GRDyccvCbm" src="https://github.com/junyong008/FilmU/assets/69251013/b143fc83-7f93-479f-8217-80eb1baf4aeb">
