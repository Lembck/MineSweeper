import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javalib.impworld.*;
import javalib.worldimages.*;
import tester.Tester;

class Utils {
  int wPadding = 50;
  int hPadding = 50;
  int squareLength = 30;
  ArrayList<Color> colors = new ArrayList<Color>(Arrays.asList(Color.gray, Color.blue, Color.green, Color.red, 
      Color.magenta.darker(), Color.red.darker(), Color.blue.brighter(),
      Color.orange.darker(), Color.yellow));

  public int posToId(Posn pos, int widthCount, int heightCount) {
    int y = (pos.y - this.hPadding) / squareLength;
    int x = (pos.x - this.wPadding) / squareLength;

    return y * widthCount + x;
  }

  public ArrayList<Integer> getNeighborIds(int id, int w, int g) {
    ArrayList<Integer> ids = new ArrayList<Integer>();

    if (id % w == 0) {
      ids = new ArrayList<Integer>(Arrays.asList(id+1, 
          id-w, id-w+1, id+w, id+w+1));
    } else if (id % w == w - 1) {
      ids = new ArrayList<Integer>(Arrays.asList(id-1, 
          id-w, id-w-1, 
          id+w, id+w-1));
    } else {
      ids = new ArrayList<Integer>(Arrays.asList(id-1, id+1, 
          id-w, id-w-1, id-w+1, 
          id+w, id+w-1, id+w+1));
    }

    ArrayList<Integer> newIds = new ArrayList<Integer>();
    for (int i : ids) {
      if (i >= 0 && i < g) {
        newIds.add(i);
      }
    }

    return newIds;
  }



}

class Square {
  int id;
  int neighboringBombCount;
  boolean isBomb;
  boolean isCovered;
  boolean isFlagged;

  ArrayList<Square> neighbors;

  Utils c = new Utils();
  public int neighboringFlagCount;
  public int neighboringCoveredCount;

  Square(int id) {
    this.id = id;
    this.isCovered = true;
    this.isBomb = false;
    this.neighboringBombCount = 0;
    this.neighboringFlagCount = 0;
    this.neighbors = new ArrayList<Square>();
  }

  public void setBomb() {
    this.isBomb = true;
  }

  public WorldImage toImage() {
    WorldImage img = new EmptyImage();

    if (this.isFlagged) {
      img = new OverlayOffsetAlign(
          AlignModeX.CENTER, AlignModeY.MIDDLE, 
          new OverlayOffsetAlign(
              AlignModeX.CENTER, AlignModeY.MIDDLE, new EquilateralTriangleImage((c.squareLength - 2) / 2, OutlineMode.SOLID, Color.red),
              0, 0, new RectangleImage(c.squareLength - 2, c.squareLength - 2, OutlineMode.SOLID, Color.lightGray)),
          0, 0, new RectangleImage(c.squareLength, c.squareLength, OutlineMode.SOLID, Color.black));
    } else if (this.isCovered) {
      img = new OverlayOffsetAlign(
          AlignModeX.CENTER, AlignModeY.MIDDLE, new RectangleImage(c.squareLength - 2, c.squareLength - 2, OutlineMode.SOLID, Color.lightGray),
          0, 0, new RectangleImage(c.squareLength, c.squareLength, OutlineMode.SOLID, Color.black));
    } else if (this.isBomb) {
      img = new OverlayOffsetAlign(
          AlignModeX.CENTER, AlignModeY.MIDDLE, new CircleImage((c.squareLength - 2) / 2, OutlineMode.SOLID, Color.red),
          0, 0, new RectangleImage(c.squareLength, c.squareLength, OutlineMode.SOLID, Color.black));
    } else {
      img = new OverlayOffsetAlign(
          AlignModeX.CENTER, AlignModeY.MIDDLE, 
          new OverlayOffsetAlign(
              AlignModeX.CENTER, AlignModeY.MIDDLE, new TextImage(Integer.toString(this.neighboringBombCount), c.colors.get(this.neighboringBombCount)),
              0, 0, new RectangleImage(c.squareLength - 2, c.squareLength - 2, OutlineMode.SOLID, Color.gray)),
          0, 0, new RectangleImage(c.squareLength, c.squareLength, OutlineMode.SOLID, Color.black));
    }
    
    return img;
  }

  public void setUncovered() {
    if (this.isCovered) {
      this.isCovered = false;
      for (Square n : this.neighbors) {
        n.neighboringCoveredCount -= 1;
      }
    }
  }

  public int toggleFlagged() {
    if (this.isFlagged) {
      this.isFlagged = false;
      for (Square n : this.neighbors) {
        n.neighboringFlagCount -= 1;
      }
      return -1;
    } 
    this.isFlagged = true;
    for (Square n : this.neighbors) {
      n.neighboringFlagCount += 1;
    }
    return 1;
  }

  public void setNeighbors(ArrayList<Integer> neighborIds, ArrayList<Square> grid) {
    for (int i : neighborIds) {
      this.neighbors.add(grid.get(i));
    }
    this.neighboringCoveredCount = this.neighbors.size();
  }
}

class MineSweeper extends World {
  int widthCount;
  int heightCount;
  int bombCount;
  int gridCount;

  int width;
  int height;

  int flagCount;
  
  Posn mouseCoords;

  boolean gameover;
  boolean won;

  Random r;

  ArrayList<Square> grid;

  Utils c = new Utils();

  MineSweeper(int widthCount, int heightCount, int bombCount) {
    this.widthCount = widthCount;
    this.heightCount = heightCount;
    this.bombCount = bombCount;
    this.gridCount = widthCount * heightCount;
    this.grid = new ArrayList<Square>();
    this.mouseCoords = new Posn(0, 0);

    this.r = new Random();

    this.calculateWidth();
    this.calculateHeight();

    this.createGrid();
    this.setNeighbors();
    
    this.setUp();

  }
  
  public void setUp() {
    this.gameover = false;
    this.won = false;
    this.flagCount = 0;

    this.setBombs();
    this.countNeighboringBombs();
  }

  public void resetGame() {
    for (Square s : this.grid) {
      s.isCovered = true;
      s.neighboringBombCount = 0;
      s.neighboringFlagCount = 0;
      s.neighboringCoveredCount = s.neighbors.size();
      s.isBomb = false;
      s.isFlagged = false;
    }
    
    this.setUp();
  }

  private void setNeighbors() {
    for (Square s : this.grid) {
      ArrayList<Integer> neighborIds = new Utils().getNeighborIds(s.id, this.widthCount, this.gridCount);
      s.setNeighbors(neighborIds, this.grid);
    }
  }

  private void countNeighboringBombs() {
    for (Square s : this.grid) {
      if (!s.isBomb) {
        for (Square nS : s.neighbors) {
          if (nS.isBomb) {
            s.neighboringBombCount++;
          }
        }
      }
    }
  }
  
  

  private void calculateWidth() {
    this.width = this.widthCount * c.squareLength + c.wPadding * 2;
  }

  private void calculateHeight() {
    this.height = this.heightCount * c.squareLength + c.hPadding * 2;
  }

  private void createGrid() {
    for (int rIndex = 0; rIndex < this.heightCount; rIndex++) {
      for (int cIndex = 0; cIndex < this.widthCount; cIndex++) {
        int id = rIndex * this.widthCount + cIndex;
        this.grid.add(new Square(id));
      }
    }
  }

  private void setBombs() {
    for (int i = 0; i < this.bombCount; i++) {
      boolean succeded = false;
      while (!succeded) {
        Square s = this.grid.get(r.nextInt(this.gridCount));
        if (!s.isBomb) {
          s.setBomb();
          succeded = true;
        }
      }
    }
  }

  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width, this.height);

    WorldImage grid = new EmptyImage();

    for (int rIndex = 0; rIndex < this.heightCount; rIndex++) {
      WorldImage row = new EmptyImage();
      for (int cIndex = 0; cIndex < this.widthCount; cIndex++) {
        int id = rIndex * this.widthCount + cIndex;
        row = new BesideImage(row, this.grid.get(id).toImage());
      }
      grid = new AboveImage(grid, row);
    }

    ws.placeImageXY(grid, this.width/2, this.height/2);
    ws.placeImageXY(new TextImage(Integer.toString(this.bombCount - this.flagCount), Color.black), this.width/2, c.hPadding / 2);

    if (this.gameover) {
      String s = "You Lost!";
      if (this.won) { s = "You Won!"; }
      ws.placeImageXY(new TextImage(s, Color.black), this.width/2, this.height - c.hPadding/2);
    }

    return ws;
  }


  public void onLeftClicked(Square s) {
    if (!s.isFlagged) {
      if (s.isBomb) {
        this.gameover = true;
        for (Square otherS : this.grid) {
          otherS.isFlagged = false;
          otherS.setUncovered();
        }
      } else if (s.neighboringBombCount == 0) {
        this.expandUncovered(s.id);
        s.setUncovered();
      } else {
        s.setUncovered();
      }
      
    }
  }

  private void onRightClicked(Square s) {
    if (s.isCovered) {
      this.flagCount += s.toggleFlagged();
      this.checkOver();
    }
  }

  public void onMouseClicked(Posn pos, String buttonName) {
    int id = new Utils().posToId(pos, this.widthCount, this.heightCount);

    if (this.gameover) {
      this.resetGame();
    } else if (id >= 0 && id < this.gridCount) {
      Square s = this.grid.get(id);
      if (buttonName == "LeftButton") {
        this.onLeftClicked(s);
      } else if (buttonName == "RightButton") {
        this.onRightClicked(s);
      }
    }
  }
  
  public void onKeyEvent(String key) {
    if (key.equals(" ")) {
      int id = new Utils().posToId(this.mouseCoords, this.widthCount, this.heightCount);
      if (this.gameover) {
        this.resetGame();
      } else if (id >= 0 && id < this.gridCount) {
        Square s = this.grid.get(id);
        if (!s.isCovered && s.neighboringBombCount != 0 && s.neighboringBombCount == s.neighboringFlagCount) {
          for (Square n : s.neighbors) {
            this.onLeftClicked(n);
          }
        } else if (!s.isCovered && s.neighboringBombCount != 0 && s.neighboringBombCount > s.neighboringFlagCount
            && s.neighboringBombCount == s.neighboringCoveredCount) {
          System.out.println("converting " + s.neighboringFlagCount + "  " + s.neighboringBombCount + " " + s.neighboringCoveredCount);
          for (Square n : s.neighbors) {
            if (!n.isFlagged) {
              this.onRightClicked(n);
            }
          }
        } else {
          System.out.println( s.neighboringFlagCount + "  " + s.neighboringBombCount + " " + s.neighboringCoveredCount);
        }
      }
    }
  }
  
  public void onMouseMoved(Posn pos) {
    this.mouseCoords = pos;
  }

  public void checkOver() {
    if (this.bombCount == this.flagCount) {
      this.gameover = true;
      this.won = true;
      for (Square s : this.grid) {
        if ((s.isBomb && !s.isFlagged) || (s.isFlagged && !s.isBomb)) {
          this.won = false;
        }
      }
    }
  }

  private void expandUncovered(int id) {
    for (Square s : this.grid.get(id).neighbors) {
      if (!s.isBomb && s.isCovered) {
        s.setUncovered();
        if (s.neighboringBombCount == 0) {
          expandUncovered(s.id);
        }
      }
    }
  }

  public WorldScene lastScene(String s) {
    for (Square otherS : this.grid) {
      otherS.isFlagged = false;
      otherS.setUncovered();
    }
    return this.makeScene();
  }

}

class Examples {
  Examples() {}

  MineSweeper world = new MineSweeper(30, 16, 99);

  void testBigBang(Tester t) {
    int worldWidth = this.world.width;
    int worldHeight = this.world.height;
    double tickRate = 0.03;
    this.world.bigBang(worldWidth, worldHeight, tickRate);
  }
}