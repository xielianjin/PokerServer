package com.hyphenated.card.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hyphenated.card.AbstractSpringTest;
import com.hyphenated.card.dao.GameDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.CommonTournamentFormats;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.GameStructure;
import com.hyphenated.card.domain.GameType;
import com.hyphenated.card.domain.HandEntity;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHand;

public class HandServiceTest extends AbstractSpringTest {

	@Autowired
	private PokerHandService handService;
	
	@Autowired
	private GameDao gameDao;
	@Autowired
	private PlayerDao playerDao;
	
	@Test
	public void verifyGameSetup(){
		Game game = setupGame();
		assertTrue(game.getId() > 0);
		assertEquals(4,game.getPlayers().size());
		assertEquals(BlindLevel.BLIND_10_20, game.getGameStructure().getCurrentBlindLevel());
		assertNull(game.getGameStructure().getCurrentBlindEndTime());
		assertTrue(game.isStarted());
		
		for(Player p : game.getPlayers()){
			assertTrue(p.getGamePosition() > 0);
		}
	}
	
	@Test
	public void testFirstHandInGame(){
		Game game = setupGame();
		assertEquals(BlindLevel.BLIND_10_20, game.getGameStructure().getCurrentBlindLevel());
		assertNull(game.getGameStructure().getCurrentBlindEndTime());
		
		HandEntity hand = handService.startNewHand(game);
		assertTrue(hand.getId() > 0);
		assertEquals(BlindLevel.BLIND_10_20, hand.getBlindLevel());
		assertNotNull(game.getGameStructure().getCurrentBlindEndTime());
		assertEquals(hand.getBlindLevel(), game.getGameStructure().getCurrentBlindLevel());
		assertEquals(4, hand.getPlayers().size());
		assertNotNull(hand.getBoard());
		assertTrue(hand.getBoard().getId() > 0);
		assertNull(hand.getBoard().getFlop1());
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(players.get(0).getPlayer(), game.getPlayerInBTN());
		assertEquals(players.get(1).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInBB(hand));
	}
	
	@Test
	public void testBlindLevelIncrease(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		assertEquals(BlindLevel.BLIND_10_20, hand.getBlindLevel());
		assertNotNull(game.getGameStructure().getCurrentBlindEndTime());
		long firstBoardId = hand.getBoard().getId();
		
		game.getGameStructure().setCurrentBlindEndTime(new Date(new Date().getTime() - 100));
		HandEntity nextHand = handService.startNewHand(game);
		
		assertEquals(BlindLevel.BLIND_15_30, nextHand.getBlindLevel());
		assertEquals(nextHand.getBlindLevel(), game.getGameStructure().getCurrentBlindLevel());
		assertTrue(game.getGameStructure().getCurrentBlindEndTime().getTime() > new Date().getTime());
		assertTrue(nextHand.getBoard().getId() != firstBoardId);
	}
	
	@Test
	public void testFlop(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		assertNull(hand.getBoard().getFlop1());
		
		hand = handService.flop(hand);
		assertTrue(hand.getBoard().getFlop1() != null);
		assertTrue(hand.getBoard().getFlop2() != null);
		assertTrue(hand.getBoard().getFlop3() != null);
	}
	
	@Test
	public void testTurn(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		assertNull(hand.getBoard().getTurn());
		assertNotNull(hand.getBoard().getFlop1());
		
		hand = handService.turn(hand);
		assertNotNull(hand.getBoard().getTurn());
		assertNull(hand.getBoard().getRiver());
	}
	
	@Test
	public void testRiver(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		hand = handService.turn(hand);
		hand = handService.river(hand);
		
		assertNotNull(hand.getBoard().getFlop3());
		assertNotNull(hand.getBoard().getTurn());
		assertNotNull(hand.getBoard().getRiver());
	}
	
	@Test(expected=IllegalStateException.class)
	public void testDuplicateFlop(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		hand = handService.flop(hand);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testFaildedTurn(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.turn(hand);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testDuplicateTurn(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		hand = handService.turn(hand);
		hand = handService.turn(hand);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testFailedRiver(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		hand = handService.river(hand);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testDuplicateRiver(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		hand = handService.flop(hand);
		hand = handService.turn(hand);
		hand = handService.river(hand);
		hand = handService.river(hand);
	}
	
	@Test
	public void testNextToActAtStart(){
		Game game = setupGame();
		Player btnPlayer = game.getPlayerInBTN();
		assertNotNull(btnPlayer);
		
		HandEntity hand = handService.startNewHand(game);
		Player bbPlayer = handService.getPlayerInBB(hand);
		assertNotNull(bbPlayer);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(btnPlayer, players.get(0).getPlayer());
		assertEquals(bbPlayer, players.get(2).getPlayer());
		assertEquals("Check Next Player to Act is after BB", players.get(3).getPlayer(), hand.getCurrentToAct());
	}
	
	@Test
	public void testEndHand(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		Player bbPlayer = handService.getPlayerInBB(hand);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(bbPlayer, players.get(2).getPlayer());
		
		handService.endHand(hand);
		assertEquals(game.getPlayerInBTN(), players.get(1).getPlayer());
		
		hand = handService.startNewHand(game);
		assertEquals(players.get(3).getPlayer(), handService.getPlayerInBB(hand));
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInSB(hand));
		
		handService.endHand(hand);
		hand = handService.startNewHand(game);
		assertEquals(game.getPlayerInBTN(), players.get(2).getPlayer());
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInBB(hand));
		assertEquals(players.get(3).getPlayer(), handService.getPlayerInSB(hand));
		
		handService.endHand(hand);
		hand = handService.startNewHand(game);
		assertEquals(game.getPlayerInBTN(), players.get(3).getPlayer());
		assertEquals(players.get(1).getPlayer(), handService.getPlayerInBB(hand));
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInSB(hand));
		
		handService.endHand(hand);
		hand = handService.startNewHand(game);
		assertEquals(game.getPlayerInBTN(), players.get(0).getPlayer());
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInBB(hand));
		assertEquals(players.get(1).getPlayer(), handService.getPlayerInSB(hand));
	}
	
	@Test
	public void testEndHandWithElimination(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		
		players.get(2).getPlayer().setChips(0);

		flushAndClear();
		
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		assertEquals("Less One player", players.size() - 1, hand.getPlayers().size());
		assertEquals(game.getPlayerInBTN(), players.get(1).getPlayer());
		assertEquals(players.get(3).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInBB(hand));
	}
	
	@Test
	public void testEndHandWithEliminationSB(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		
		players.get(1).getPlayer().setChips(0);

		flushAndClear();
		
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		assertEquals("Less One player", players.size() - 1, hand.getPlayers().size());
		assertEquals(game.getPlayerInBTN(), players.get(2).getPlayer());
		assertEquals(players.get(3).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInBB(hand));
	}
	
	@Test
	public void testEndHandWithEliminationNextBB(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		
		players.get(3).getPlayer().setChips(0);

		flushAndClear();
		
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		assertEquals("Less One player", players.size() - 1, hand.getPlayers().size());
		assertEquals(game.getPlayerInBTN(), players.get(1).getPlayer());
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInBB(hand));
	}
	
	@Test
	public void testEndHandWithEliminationToHeadsUp(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		
		players.get(3).getPlayer().setChips(0);
		players.get(0).getPlayer().setChips(0);

		flushAndClear();
		
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		assertEquals("Less Two players", players.size() - 2, hand.getPlayers().size());
		assertEquals(game.getPlayerInBTN(), players.get(1).getPlayer());
		assertEquals(players.get(1).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInBB(hand));	
	}
	
	@Test
	public void testEndHandWithEliminationToHeadsUpOther(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		
		players.get(3).getPlayer().setChips(0);
		players.get(1).getPlayer().setChips(0);

		flushAndClear();
		
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		assertEquals("Less Two players", players.size() - 2, hand.getPlayers().size());
		assertEquals(game.getPlayerInBTN(), players.get(2).getPlayer());
		assertEquals(players.get(2).getPlayer(), handService.getPlayerInSB(hand));
		assertEquals(players.get(0).getPlayer(), handService.getPlayerInBB(hand));	
	}
	
	@Test
	public void testHandSetupWithChipBetsAndBlinds(){
		Game game = setupGame();
		HandEntity hand = handService.startNewHand(game);
		int smallBlind = hand.getBlindLevel().getSmallBlind();
		int bigBlind = hand.getBlindLevel().getBigBlind();
		assertEquals(smallBlind + bigBlind , hand.getPot());
		assertEquals(bigBlind, hand.getTotalBetAmount());
		
		List<PlayerHand> players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		assertEquals(handService.getPlayerInBB(hand), players.get(2).getPlayer());
		assertEquals(bigBlind, players.get(2).getBetAmount());
		assertEquals(bigBlind, players.get(2).getRoundBetAmount());
		assertEquals(2000 - bigBlind, players.get(2).getPlayer().getChips());
		
		assertEquals(handService.getPlayerInSB(hand), players.get(1).getPlayer());
		assertEquals(smallBlind, players.get(1).getBetAmount());
		assertEquals(smallBlind, players.get(1).getRoundBetAmount());
		assertEquals(2000 - smallBlind, players.get(1).getPlayer().getChips());
		
		flushAndClear();
		
		//End game and start a new one.  See that chip stacks and pot size are correct for new hand
		game = gameDao.findById(game.getId());
		handService.endHand(game.getCurrentHand());
		hand = handService.startNewHand(game);
		players = new ArrayList<PlayerHand>();
		players.addAll(hand.getPlayers());
		Collections.sort(players);
		
		assertEquals(handService.getPlayerInBB(hand), players.get(3).getPlayer());
		assertEquals(bigBlind, players.get(3).getBetAmount());
		assertEquals(2000 - bigBlind, players.get(3).getPlayer().getChips());
		
		assertEquals(handService.getPlayerInSB(hand), players.get(2).getPlayer());
		assertEquals(smallBlind, players.get(2).getBetAmount());
		assertEquals(2000 - bigBlind - smallBlind, players.get(2).getPlayer().getChips());
		
		assertEquals(0, players.get(1).getBetAmount());
		assertEquals(2000 - smallBlind, players.get(1).getPlayer().getChips());
		
		//Flop should clear betting round amount values
		assertEquals(bigBlind, hand.getTotalBetAmount());
		hand = handService.flop(hand);
		assertEquals(0, hand.getTotalBetAmount());
	}
	
	private Game setupGame(){
		Game game = new Game();
		game.setName("Test Game");
		game.setPlayersRemaining(4);
		game.setStarted(true);
		game.setGameType(GameType.TOURNAMENT);
		GameStructure gs = new GameStructure();
		CommonTournamentFormats format =  CommonTournamentFormats.TWO_HR_SIXPPL;
		gs.setBlindLength(format.getTimeInMinutes());
		gs.setBlindLevels(format.getBlindLevels());
		gs.setStartingChips(2000);
		gs.setCurrentBlindLevel(BlindLevel.BLIND_10_20);
		game.setGameStructure(gs);
		
		Player p1 = new Player();
		p1.setName("Player 1");
		p1.setGame(game);
		p1.setChips(gs.getStartingChips());
		p1.setGamePosition(1);
		playerDao.save(p1);
		
		Player p2 = new Player();
		p2.setName("Player 2");
		p2.setGame(game);
		p2.setChips(gs.getStartingChips());
		p2.setGamePosition(2);
		playerDao.save(p2);
		
		Player p3 = new Player();
		p3.setName("Player 3");
		p3.setGame(game);
		p3.setChips(gs.getStartingChips());
		p3.setGamePosition(3);
		playerDao.save(p3);
		
		Player p4 = new Player();
		p4.setName("Player 4");
		p4.setGame(game);
		p4.setChips(gs.getStartingChips());
		p4.setGamePosition(4);
		playerDao.save(p4);
		
		game.setPlayerInBTN(p1);
		game = gameDao.save(game);
		
		flushAndClear();
		
		return gameDao.findById(game.getId());
	}
}