package edu.cnm.deepdive.dominionendpointtestspring.controller;

import edu.cnm.deepdive.dominionendpointtestspring.GameStateInfoTransferObject;
import edu.cnm.deepdive.dominionendpointtestspring.model.aggregates.GameStateInfo;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Game;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Player;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Turn;
import edu.cnm.deepdive.dominionendpointtestspring.model.pojo.Card;
import edu.cnm.deepdive.dominionendpointtestspring.model.pojo.Card.CardType;
import edu.cnm.deepdive.dominionendpointtestspring.service.GameLogic;
import edu.cnm.deepdive.dominionendpointtestspring.state.GameEvents;
import edu.cnm.deepdive.dominionendpointtestspring.state.GameStates;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@ExposesResourceFor(Game.class)
public class GameController {
 @Autowired
 private StateMachine<GameStates, GameEvents> stateMachine;
  //private final GameRepository gameRepository;

  private GameLogic gameLogic;
 @Autowired
  public GameController() {

  }

  @PostMapping(value="/newgame")
  public GameStateInfoTransferObject newGame(){
   stateMachine.sendEvent(GameEvents.START_GAME);
   GameStateInfoTransferObject gameStateInfoTransferObject = setupDummyGame();
   return gameStateInfoTransferObject;
  }

 private GameStateInfoTransferObject setupDummyGame() {
  Game game = new Game();
  Player player1 = new Player( "Erica");
  Player player2 = new Player( "Danny");
  ArrayList<Player> players = new ArrayList<>();
  players.add(player1);
  players.add(player2);
  game.setPlayers(players);
  GameStateInfo gameState = new GameStateInfo(game, new Turn(game, player1));
  return buildTransferObject(gameState);
 }

 @GetMapping(value="/getstate")
 public String getState(){
  return stateMachine.getState().getId().toString();
 }

 @PostMapping("/{gameid}/{playerid}/endphase")
 public String endPhase(@PathVariable("gameid") long gameId, @PathVariable("playerid")long playerId){
  boolean isOver;
  switch(stateMachine.getState().getId()){
   case PLAYER_1_DISCARDING:
    stateMachine.sendEvent(GameEvents.PLAYER_1_END_DISCARDS);
    break;
   case PLAYER_1_ACTION:
    stateMachine.sendEvent(GameEvents.PLAYER_1_END_ACTIONS);
    break;
   case PLAYER_1_BUYING:
    isOver = gameLogic.testForVictory();
    if (isOver) {
     stateMachine.sendEvent(GameEvents.END_GAME);
    } else {
     stateMachine.sendEvent(GameEvents.PLAYER_1_END_BUYS);
    }
    break;
   case PLAYER_2_DISCARDING:
    stateMachine.sendEvent(GameEvents.PLAYER_2_END_DISCARDS);
    break;
   case PLAYER_2_ACTION:
    stateMachine.sendEvent(GameEvents.PLAYER_2_END_ACTIONS);
    break;
   case PLAYER_2_BUYING:
     isOver = gameLogic.testForVictory();
    if (isOver) {
     stateMachine.sendEvent(GameEvents.END_GAME);
    } else {
     stateMachine.sendEvent(GameEvents.PLAYER_2_END_BUYS);
    }
    break;
   default:
    return "Invalid Request";
  }
  return   stateMachine.getState().getId().toString();
  }


  @GetMapping(value = "/gamestateinfo")
  public GameStateInfoTransferObject getGameInfo(){
   GameStateInfoTransferObject gameStateInfoTransferObject = setupDummyGame();
   return gameStateInfoTransferObject;
  }

 private GameStateInfoTransferObject buildTransferObject(GameStateInfo gameState) {
  return new GameStateInfoTransferObject(
      gameState.getCurrentPlayerStateInfo().getHand().getCardsInHand(),
      gameState.getCurrentPlayerStateInfo().getPlayer().getPlayerScore(),
      gameState.getPlayerStateInfoPlayer2().getPlayer().getPlayerScore(),
      gameState.getCurrentPlayerStateInfo().getPlayer().getNumAction(),
      gameState.getCurrentPlayerStateInfo().getPlayer().getNumBuy(),
      gameState.getCurrentPlayerStateInfo().calculateBuyingPower(),
      gameState.getGame().getStacks(),
      gameState.getPlaysInPreviousTurn(),
      stateMachine.getState().getId().toString()
  );
 }




/**
 @PostMapping("{cardid}/buy")
 public GameStateInfo playerBuysTarget(@PathVariable int gameId, int playerId, CardType cardType ){
  //return gameLogic.buyTarget(cardType, playerId, gameId);
  return new GameStateInfo();
 }


*/
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoSuchElementException.class)
  public void notFound() {
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(Exception.class)
  public void badRequest() {
  }
/**
 @Bean
 public DefaultStateMachineAdapter<GameStates, GameEvents, Game> orderStateMachineAdapter(
     StateMachineFactory<GameStates, GameEvents> stateMachineFactory,
     StateMachinePersister<GameStates, GameEvents, ContextEntity<GameStates, GameEvents, Serializable>> persister) {
  return new DefaultStateMachineAdapter<GameStates, GameEvents, Game>(stateMachineFactory, persister);
 }

 @Bean
 public ContextObjectResourceProcessor<GameStates, GameEvents, Game> orderResourceProcessor(
     EntityLinks entityLinks,
     DefaultStateMachineAdapter<GameStates, GameEvents, ContextEntity<GameStates, GameEvents, Game>> gameStateMachineAdapter) {
  return new ContextObjectResourceProcessor<GameStates, GameEvents, Game>(entityLinks, gameStateMachineAdapter);
 }

 @Bean
 public DefaultStateMachinePersister<GameStates, GameEvents, Game> persister(
     StateMachinePersist<GameStates, GameEvents, ContextEntity<GameStates, GameEvents, Serializable>> persist) {
  return new DefaultStateMachinePersister<GameStates, GameEvents, Game>(persist);
 }

 @Bean
 public StateMachinePersist<GameStates,GameEvents, Game> persist() {
  return new StateMachinePersist<GameStates, GameEvents, Game>() {

   @Override
   public void write(StateMachineContext<GameStates, GameEvents> stateMachineContext, Game game)
       throws Exception {
    game.setStateMachineContext(stateMachineContext);
   }

   @Override
   public StateMachineContext<GameStates, GameEvents> read(Game game) throws Exception {
    return game.getStateMachineContext();
   }




  };
 }
 */
}
