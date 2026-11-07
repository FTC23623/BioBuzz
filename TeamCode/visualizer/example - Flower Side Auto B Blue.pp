{
  "version": "2.0.0",
  "startPoint": {"x":55.75,"y":132.50,"headingDeg":90.00},
  "lines": [
    {"id":"path1","name":"to spot 1","color":"#d99a00","kind":"atomic","endPoint":{"x":54.65,"y":117.10,"name":"to spot 1"},"controlPoints":[],"heading":{"type":"linear","startDeg":90.00,"endDeg":78.00}},
    {"id":"path2","name":"to spot 2","color":"#2f6fdf","kind":"atomic","endPoint":{"x":39.75,"y":127.70,"name":"to spot 2"},"controlPoints":[],"heading":{"type":"linear","startDeg":78.00,"endDeg":88.00}},
    {"id":"path3","name":"to spot 3","color":"#cc3d3d","kind":"atomic","endPoint":{"x":39.15,"y":121.20,"name":"to spot 3"},"controlPoints":[],"heading":{"type":"linear","startDeg":88.00,"endDeg":102.00}},
    {"id":"path4","name":"to spot 4","color":"#2e9e5b","kind":"atomic","endPoint":{"x":7.85,"y":118.10,"name":"to spot 4"},"controlPoints":[],"heading":{"type":"linear","startDeg":102.00,"endDeg":119.00}}
  ],
  "shapes": [],
  "sequence": [
    {"kind":"path","lineId":"path1"},
    {"kind":"wait","id":"wait1","name":"LAUNCH WHEN TAG SEEN","durationMs":2000},
    {"kind":"path","lineId":"path2"},
    {"kind":"wait","id":"wait2","name":"FLOWER","durationMs":2300},
    {"kind":"path","lineId":"path3"},
    {"kind":"wait","id":"wait3","name":"LAUNCH","durationMs":2000},
    {"kind":"path","lineId":"path4"}
  ],
  "fieldPoints": [],
  "robot": {"rLength":18.00,"rWidth":18.00},
  "activePaths": []
}
