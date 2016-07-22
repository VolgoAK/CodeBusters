import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        BusterManager manager = new BusterManager(myTeamId);
        // game loop
        while (true) {
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                System.err.println(entityType + "en type");
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.
                manager.addEntity(entityId, new Point(x, y), entityType, state, value);
            }
            manager.makeTurn();
            manager.move();
        }
    }
}

class BusterManager{
    static Point basePosition;
    static int teamId;

    static Map<Integer, MyBuster> myBusters = new HashMap<>();
    static Map<Integer, Buster> enemyBusters = new HashMap<>();
    static  Map<Integer, Ghost> ghosts = new HashMap<>();
    static List<BoardPoint> boardPoints = new ArrayList<>();

    int turn = 0;

    BusterManager(int teamId){
        this.teamId = teamId;

        basePosition = (teamId == 0) ? new Point(1000, 1000) : new Point(15000, 8000);

        // Create board points for exploring
        Collections.addAll(boardPoints, new BoardPoint(2000, 4000), new BoardPoint(1500, 7500), new BoardPoint(5000, 2000)
                , new BoardPoint(5000, 4500), new BoardPoint(5000, 7000), new BoardPoint(8000, 2000),
                new BoardPoint(8000, 4500), new BoardPoint(8000, 7000), new BoardPoint(11000, 2000), new BoardPoint(11000, 4500),
                new BoardPoint(11000, 7000), new BoardPoint(14500, 1500), new BoardPoint(14000, 5000));
    }

    /**
     * Sets behavior type for all busters
     */
    void initializeBusters(){
        Set<Integer> ids = myBusters.keySet();
        for(int id : ids){
            myBusters.get(id).setBehavior(new SimpleHunter());
        }
    }

    // TODO: 20.07.2016 make method which set visible for every ghost to false, increase time since visible
    // TODO: 21.07.2016 delete ghosts from map when caught

    /**
     * Prepares turns for myBusters
     * makes all entities invisible
     */
    void makeTurn(){
        if(turn == 0)initializeBusters();
        Set<Integer> ids = myBusters.keySet();
        for(int id : ids){
            myBusters.get(id).makeMove();
            myBusters.get(id).stunCountDown();
        }
        ids = ghosts.keySet();
        for(int id : ids){
            ghosts.get(id).visible = false;
        }
        ids = enemyBusters.keySet();
        for(int id : ids){
            enemyBusters.get(id).visible = false;
            enemyBusters.get(id).stunCountDown();
        }
        turn++;
    }

    /**
     *Outputs move for every my buster
     */
    void move(){
        Set<Integer> ids = myBusters.keySet();
        for(int id : ids){
            System.out.println( myBusters.get(id).move);
        }
    }

    /**
     * Adds entities to maps
     * Decides where to put an entity by type
     */
    void addEntity(int id, Point position, int type, int state, int value){
        if(type == -1){
            System.err.println("add ghost");
            addGhost(id, position, state, value);
        }else if(type == teamId){
            addMyBuster(id, position, state, value);
        }else{
            addEnemyBuster(id, position, state, value);
        }
    }

    void addMyBuster(int id, Point position, int state, int value){
        MyBuster buster;

        if(myBusters.containsKey(id)){
            buster = myBusters.get(id);
            buster.position = position;
            buster.state = state;
            buster.value = value;
        }else{
            buster = new MyBuster(position, id);
            buster.state = state;
            myBusters.put(id, buster);
        }
    }

    void addEnemyBuster(int id, Point position, int state, int value){
        Buster buster;

        if(enemyBusters.containsKey(id)){
            buster = enemyBusters.get(id);
            buster.position = position;
            buster.state = state;
            buster.value = value;
            buster.visible = true;
        }else{
            buster = new Buster(position, id);
            buster.state = state;
            buster.value = value;
            buster.visible = true;
            enemyBusters.put(id, buster);
        }
    }

    void addGhost(int id, Point position, int state, int value){
        Ghost ghost;

        if(ghosts.containsKey(id)){
            ghost = ghosts.get(id);
            ghost.visible = true;
            ghost.position = position;
            ghost.value = value;
            ghost.turnsSinseWasVisible = 0;
        }else{
            ghost = new Ghost(position, id);
            ghost.visible = true;
            ghost.value = value;
            ghosts.put(id, ghost);
        }
    }
}

/**
 * Enemy busters and base class for MyBusters
 */
class Buster{
    Point position;
    int state = 0;
    int id;
    int value;
    boolean visible;
    int stunCountDown = 0;
    int stunedCountDown = 0;

    Buster(Point point, int id){
        position = point;
        this.id = id;
    }

    void stunCountDown(){
        stunCountDown = (stunCountDown > 0) ? stunCountDown - 1 : 0;
        stunedCountDown = (stunedCountDown > 0) ? stunedCountDown - 1 : 0;
    }
}

class MyBuster extends Buster{

    String move = "MOVE " + 8000 + " " + 4000;
    BusterBehavior behavior;

    MyBuster(Point point, int id){
        super(point, id);
    }

    void setBehavior(BusterBehavior behavior){
        this.behavior = behavior;
        behavior.setBuster(this);
    }

    void makeMove(){
        move = behavior.findMove();
    }
}

/**
 * Nasty ghosts. Every one must be caught!
 */
class Ghost{
    Point position;
    Boolean visible;
    int turnsSinseWasVisible;
    int value;
    int id;

    Ghost(Point point, int id){
        position = point;
        this.id = id;
    }
}

/**
 * Represents points on the game point
 */
class BoardPoint extends Point{
    boolean explored;
    boolean taken;
    int ghostAround;

    BoardPoint(int x, int y){
       super(x, y);
    }
}


interface BusterBehavior{
    String findMove();
    void setBuster(MyBuster buster);
}

/**
 * Base behavior for busters
 * Just finds nearest ghost, attack enemy when they are close and explore points one by one
 */
class SimpleHunter implements BusterBehavior{

    MyBuster buster;
    BoardPoint boardPoint;
    Ghost targetGhost;
    String move;

    public void setBuster(MyBuster buster){
        this.buster = buster;
    }

    public String findMove(){
        String stun = attackEnemy();
        if(stun != null) return stun;
        if(buster.state == 1) return goToBase();
        if(targetGhost != null){
            if(targetGhost.visible){
                return huntTargetGhost();
            }else targetGhost = null;
        }
        String message = findTargetGhost();
        if(boardPoint != null){
            if(targetGhost != null && buster.position.distance(targetGhost.position) < 3000 /*buster.position.distance(targetGhost.position) < buster.position.distance(boardPoint.point)*/){
                boardPoint.taken = false;
                boardPoint = null;
                return huntTargetGhost();
            }
            if(buster.position.distance(boardPoint) < 100){
                boardPoint.explored = true;
                boardPoint = null;
            }else{
                return "MOVE " + boardPoint.x + " " + boardPoint.y;
            }
        }

        if(message != null){
            return message;
        }
        else return explore();
    }

    /**
     * Leads buster with a ghost to base
     */
    String goToBase(){
        if(buster.position.equals(BusterManager.basePosition)){
            return "RELEASE";
        }else{
            return "MOVE " + BusterManager.basePosition.x + " " + BusterManager.basePosition.y;
        }
    }

    /**
    * Finds ghost for hunt
     * if there is no visible ghost it finds nearest invisible ghost with
     * minimal time since visible
     */
    String findTargetGhost(){
        String move = null;
        Set<Integer> ids = BusterManager.ghosts.keySet();
        int minDistance = Integer.MAX_VALUE;
        for(Integer a : ids){
            Ghost g = BusterManager.ghosts.get(a);
            if(g.visible) {
                int dist = (int) buster.position.distance(g.position);
                if (dist < minDistance) {
                    minDistance = dist;
                    targetGhost = g;
                }
            }
        }

        if(targetGhost != null){
           return huntTargetGhost();
        }
        return null;
    }

    // TODO: 22.07.2016 delete ghost when it caught
    String findInvisibleGhost(){
        return null;
    }


    String huntTargetGhost(){
        if(buster.position.distance(targetGhost.position) > 1760){
            move = "MOVE " + targetGhost.position.x + " " + targetGhost.position.y;
            return move;
        }else if(buster.position.distance(targetGhost.position) > 900){
            move = "BUST " + targetGhost.id;
            return move;
        }else {
            int x = (targetGhost.position.x > 1000) ? targetGhost.position.x - 900 : targetGhost.position.x + 900;
            move = "MOVE " + x + " " + targetGhost.position.y;
            return move;
        }
    }

    // TODO: 22.07.2016 prefer buster which is carrying a ghost and with minimal stun timer
    String attackEnemy(){
        Set<Integer> ids = BusterManager.enemyBusters.keySet();
        for(int id : ids){
            Buster enemy = BusterManager.enemyBusters.get(id);
            if(!enemy.visible) continue;
            if(buster.position.distance(enemy.position) < 1760
                    && enemy.stunedCountDown < 2 && buster.stunCountDown == 0){
                buster.stunCountDown = 21;
                enemy.stunedCountDown = 10;
                return "STUN " + enemy.id;
            }
        }
        return null;
    }

    String explore(){
        for(BoardPoint bp : BusterManager.boardPoints){
            if(!bp.explored && !bp.taken){
                boardPoint = bp;
                bp.taken = true;
                return "MOVE " + bp.x + " " + bp.y;
            }
        }
        if(boardPoint == null){
            //make buster go to nearest point
            int randomPoint = (int)(Math.random() * BusterManager.boardPoints.size());
            boardPoint = BusterManager.boardPoints.get(randomPoint);
            return "MOVE " + boardPoint.x + " " + boardPoint.y;
        }
        return "MOVE 8000 4500 null move" ;
    }
}