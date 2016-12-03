package com.github.unchama.seichiassist.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.github.unchama.seichiassist.ActiveSkill;
import com.github.unchama.seichiassist.ActiveSkillEffect;
import com.github.unchama.seichiassist.ActiveSkillPremiumEffect;
import com.github.unchama.seichiassist.Config;
import com.github.unchama.seichiassist.SeichiAssist;
import com.github.unchama.seichiassist.Sql;
import com.github.unchama.seichiassist.data.EffectData;
import com.github.unchama.seichiassist.data.GachaData;
import com.github.unchama.seichiassist.data.MenuInventoryData;
import com.github.unchama.seichiassist.data.PlayerData;
import com.github.unchama.seichiassist.task.CoolDownTaskRunnable;
import com.github.unchama.seichiassist.util.ExperienceManager;
import com.github.unchama.seichiassist.util.Util;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class PlayerInventoryListener implements Listener {
	HashMap<UUID,PlayerData> playermap = SeichiAssist.playermap;
	List<GachaData> gachadatalist = SeichiAssist.gachadatalist;
	SeichiAssist plugin = SeichiAssist.plugin;
	private Config config = SeichiAssist.config;
	private Sql sql = SeichiAssist.plugin.sql;

	//棒メニュー
	@EventHandler
	public void onPlayerClickStickMenuEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);

		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "木の棒メニュー")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			//経験値変更用のクラスを設定
			ExperienceManager expman = new ExperienceManager(player);

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */

			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowRight")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMenuData2(player));
				return;
			}

			else if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMenuData(player));
				return;
			}

			else if(itemstackcurrent.getType().equals(Material.CHEST)){
				//レベルが足りない場合処理終了
				if( playerdata.level < SeichiAssist.config.getMineStacklevel(1)){
					player.sendMessage(ChatColor.GREEN + "整地レベルが"+SeichiAssist.config.getMineStacklevel(1)+ "以上必要です");
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					return;
				}
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMineStackMenu(player));
				return;
			}
			//スキルメニューを開く
			else if(itemstackcurrent.getType().equals(Material.ENCHANTED_BOOK)){
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				//アクティブスキルとパッシブスキルの分岐
				if(itemmeta.getDisplayName().contains("アクティブ")){
					player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
				}else if(itemmeta.getDisplayName().contains("パッシブ")){
					player.sendMessage("未実装ナリよ");
					//player.openInventory(MenuInventoryData.getPassiveSkillMenuData(player));
				}
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float) 0.8);
				return;
			}
			//整地神番付を開く
			else if(itemstackcurrent.getType().equals(Material.COOKIE)){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getRankingList(player));
				return;
			}


			//溜まったガチャ券をインベントリへ
			else if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("unchama")){
				//連打防止クールダウン処理
				if(!playerdata.gachacooldownflag){
					return;
				}else{
			        //連打による負荷防止の為クールダウン処理
					new CoolDownTaskRunnable(player,false,false,true).runTaskLater(plugin,20);
				}

				ItemStack skull = Util.getskull(Util.getName(player));
				int count = 0;
				while(playerdata.gachapoint >= config.getGachaPresentInterval() && count < 576){
					playerdata.gachapoint -= config.getGachaPresentInterval();
					if(player.getInventory().contains(skull) || !Util.isPlayerInventryFill(player)){
						Util.addItem(player,skull);
					}else{
						Util.dropItem(player,skull);
					}
					count++;
				}
				//プレイヤーデータを更新
				playerdata.lastgachapoint = playerdata.gachapoint;

				if(count > 0){
					player.sendMessage(ChatColor.GOLD + "ガチャ券" + count + "枚" + ChatColor.WHITE + "プレゼントフォーユー");
					player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
				}

				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemmeta.setLore(MenuInventoryData.GachaGetButtonLore(playerdata));
				itemstackcurrent.setItemMeta(itemmeta);
			}

			//運営からのガチャ券受け取り
			else if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("whitecat_haru")){

				//nは最新のnumofsorryforbugの値になる(上限値576個)
				int n = sql.givePlayerBug(player,playerdata);
				//0だったら処理終了
				if(n == 0){
					return;
				}

				ItemStack skull = Util.getForBugskull(Util.getName(player));
				int count = 0;
				while(n > 0){
					playerdata.numofsorryforbug--;
					if(player.getInventory().contains(skull) || !Util.isPlayerInventryFill(player)){
						Util.addItem(player,skull);
					}else{
						Util.dropItem(player,skull);
					}
					n--;
					count++;
				}

				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
				player.sendMessage(ChatColor.GREEN + "運営チームから"+count+ "枚の" + ChatColor.GOLD + "ガチャ券" + ChatColor.WHITE + "を受け取りました");

				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemmeta.setLore(MenuInventoryData.SorryGachaGetButtonLore(playerdata));
				itemstackcurrent.setItemMeta(itemmeta);
			}

			//投票特典受け取り
			else if(itemstackcurrent.getType().equals(Material.DIAMOND)){

				//nは特典をまだ受け取ってない投票分
				int n = sql.compareVotePoint(player,playerdata);
				//投票数に変化が無ければ処理終了
				if(n == 0){
					return;
				}
				//先にp_voteの値を更新しておく
				playerdata.p_givenvote += n;

				int count = 0;
				while(n > 0){
					//ここに投票1回につきプレゼントする特典の処理を書く

					//ガチャ券プレゼント処理
					ItemStack skull = Util.getVoteskull(Util.getName(player));
					for (int i = 0; i < 10; i++){
						if(player.getInventory().contains(skull) || !Util.isPlayerInventryFill(player)){
							Util.addItem(player,skull);
						}else{
							Util.dropItem(player,skull);
						}
					}

					//ピッケルプレゼント処理(レベル30になるまで)
					/*
					if(playerdata.level < 30){

					}
					*/
					ItemStack itemstack = new ItemStack(Material.DIAMOND_PICKAXE,1);
					ItemMeta itemmeta = Bukkit.getItemFactory().getItemMeta(Material.DIAMOND_PICKAXE);
					itemmeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Thanks for Voting!");
					List<String> lore = Arrays.asList("投票ありがとナス♡"
							);
					itemmeta.addEnchant(Enchantment.DIG_SPEED, 3, true);
					itemmeta.addEnchant(Enchantment.DURABILITY, 3, true);
					itemmeta.setLore(lore);
					itemstack.setItemMeta(itemmeta);
					if(!Util.isPlayerInventryFill(player)){
						Util.addItem(player,itemstack);
					}else{
						Util.dropItem(player,itemstack);
					}

					//エフェクトポイント加算処理
					playerdata.activeskilldata.effectpoint += 10;

					n--;
					count++;
				}

				player.sendMessage(ChatColor.GOLD + "投票特典" + ChatColor.WHITE + "(" + count + "票分)を受け取りました");
				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);

				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemmeta.setLore(MenuInventoryData.VoteGetButtonLore(playerdata));
				itemstackcurrent.setItemMeta(itemmeta);
			}


			//経験値を消費してプレイヤーの頭を召喚
			else if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_Villager")){
				//経験値変更用のクラスを設定
				//経験値が足りなかったら処理を終了
				if(!expman.hasExp(10000)){
					player.sendMessage(ChatColor.RED + "必要な経験値が足りません");
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					return;
				}
				//経験値消費
				expman.changeExp(-10000);

				//プレイヤーの頭作成
				ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1);
				SkullMeta skullmeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
				skull.setDurability((short) 3);
				skullmeta.setOwner(player.getName());
				skull.setItemMeta(skullmeta);

				//渡すか、落とすか
				if(player.getInventory().contains(skull) || !Util.isPlayerInventryFill(player)){
					Util.addItem(player,skull);
				}else{
					Util.dropItem(player,skull);
				}
				player.sendMessage(ChatColor.GOLD + "経験値10000を消費して自分の頭を召喚しました");
				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
			}

			else if(itemstackcurrent.getType().equals(Material.STONE_BUTTON)){
				playerdata.gachaflag = !playerdata.gachaflag;
				if(playerdata.gachaflag){
					player.sendMessage(ChatColor.GREEN + "毎分のガチャ券受け取り:ON");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
					ItemMeta itemmeta = itemstackcurrent.getItemMeta();
					List<String> lore = Arrays.asList(ChatColor.RESET + "" +  ChatColor.GREEN + "毎分受け取ります"
							, ChatColor.RESET + "" +  ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで変更"
							);
					itemmeta.setLore(lore);
					itemstackcurrent.setItemMeta(itemmeta);
				}else{
					player.sendMessage(ChatColor.RED + "毎分のガチャ券受け取り:OFF");
					player.sendMessage(ChatColor.GREEN + "ガチャ券受け取りボタンを押すともらえます");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1, 1);
					ItemMeta itemmeta = itemstackcurrent.getItemMeta();
					List<String> lore = Arrays.asList(ChatColor.RESET + "" +  ChatColor.RED + "後でまとめて受け取ります"
							, ChatColor.RESET + "" +  ChatColor.DARK_RED + "" + ChatColor.UNDERLINE + "クリックで変更"
							);
					itemmeta.setLore(lore);
					itemstackcurrent.setItemMeta(itemmeta);
				}
			}

			else if(itemstackcurrent.getType().equals(Material.DIAMOND_PICKAXE)){
				// ver0.3.2 採掘速度上昇効果トグル
				playerdata.effectflag = !playerdata.effectflag;
				if(playerdata.effectflag){
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);

					//effect追加の処理
					//実際に適用されるeffect量
					int minespeedlv = 0;
					//合計effect量
					double sum = 0;
					//最大持続時間
					int maxduration = 0;
					//effectdatalistにある全てのeffectについて計算
					for(EffectData ed :playerdata.effectdatalist){
						//effect量を加算
						sum += ed.amplifier;
						//持続時間の最大値を取得
						if(maxduration < ed.duration){
							maxduration = ed.duration;
						}
					}
					//実際のeffect値をsum-1の切り捨て整数値に設定
					minespeedlv = (int)(sum - 1);

					//実際のeffect値が0より小さいときはeffectを適用しない
					if(minespeedlv < 0){
						player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 0, 0, false, false), true);
					}else{
						player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, maxduration, minespeedlv, false, false), true);
					}

					player.sendMessage(ChatColor.GREEN + "採掘速度上昇効果:ON");
				}else{
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);

					//現在の採掘速度上昇効果を削除する
					player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 0, 0, false, false), true);

					player.sendMessage(ChatColor.RED + "採掘速度上昇効果:OFF");
				}
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemstackcurrent.setItemMeta(MenuInventoryData.EFButtonMeta(playerdata,itemmeta));
			}

			else if(itemstackcurrent.getType().equals(Material.FLINT_AND_STEEL)){
				// 死亡メッセージ表示トグル
				playerdata.dispkilllogflag = !playerdata.dispkilllogflag;
				if(playerdata.dispkilllogflag){
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
					player.sendMessage(ChatColor.GREEN + "死亡メッセージ:表示");
				}else{
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
					player.sendMessage(ChatColor.RED + "死亡メッセージ:隠す");
				}
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemstackcurrent.setItemMeta(MenuInventoryData.dispKillLogToggleMeta(playerdata,itemmeta));
			}

			else if(itemstackcurrent.getType().equals(Material.IRON_SWORD)){
				// 死亡メッセージ表示トグル
				playerdata.pvpflag = !playerdata.pvpflag;
				if(playerdata.pvpflag){
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
					player.sendMessage(ChatColor.GREEN + "PvP:ON");
				}else{
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
					player.sendMessage(ChatColor.RED + "PvP:OFF");
				}
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemstackcurrent.setItemMeta(MenuInventoryData.dispPvPToggleMeta(playerdata,itemmeta));
			}

			else if(itemstackcurrent.getType().equals(Material.BEACON)){
				// spawnコマンド実行
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.chat("/spawn");
			}

			else if(itemstackcurrent.getType().equals(Material.BED)){
				// sethomeコマンド実行
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();

				if(itemmeta.getDisplayName().contains("サブホームポイント")){//ホームボタンかサブホームボタンか判定
					//ホームをセット
					int z = Integer.parseInt( itemmeta.getDisplayName().substring(15, 16) ) - 1;	//サブホームボタンの番号
					playerdata.SetSubHome(player.getLocation(), z);

					//mysqlにも書き込んどく
					/*別スレッド処理PlayerDataSaveTaskRunnableに移動
					if(!sql.UpDataSubHome(playerdata.SubHomeToString())){
						player.sendMessage(ChatColor.RED + "失敗");
					}else{
						player.sendMessage("現在位置をサブホームポイント"+(z+1)+"に設定しました");
					}
					*/
					player.sendMessage("現在位置をサブホームポイント"+(z+1)+"に設定しました");
					player.closeInventory();
				}else {
					player.chat("/sethome");
				}
			}

			else if(itemstackcurrent.getType().equals(Material.COMPASS)){
				// homeコマンド実行
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);

				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				if(itemmeta.getDisplayName().contains("サブホームポイント")){//ホームボタンかサブホームボタンか判定
					//サブホームに移動
					int z = Integer.parseInt( itemmeta.getDisplayName().substring(15, 16) ) - 1;	//サブホームボタンの番号
					Location l = playerdata.GetSubHome(z);
					if(l != null){
						World world = Bukkit.getWorld(l.getWorld().getName());
						if(world != null){
							player.teleport(l);
							player.sendMessage("サブホームポイント"+ (z+1) +"にワープしました");
						}else{
							player.sendMessage("サブホームポイント"+ (z+1) +"が設定されてません");
						}
					}else{
						player.sendMessage("サブホームポイント"+ (z+1) +"が設定されてません");
					}
				}else {
					player.chat("/home");
				}



			}

			else if(itemstackcurrent.getType().equals(Material.WORKBENCH)){
				// /fc craftコマンド実行
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.chat("/fc craft");
			}

			else if(itemstackcurrent.getType().equals(Material.WOOD_AXE)){
				// wand召喚
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.chat("//wand");
				player.sendMessage(ChatColor.RESET + "" +  ChatColor.DARK_GREEN + "" + ChatColor.UNDERLINE + "保護のかけ方\n"
						+ ChatColor.RESET + "" +  ChatColor.GREEN + "①召喚された斧を手に持ちます\n"
						+ ChatColor.RESET + "" +  ChatColor.GREEN + "②保護したい領域の一方の角を" + ChatColor.YELLOW + "左" + ChatColor.GREEN + "クリック\n"
						+ ChatColor.RESET + "" +  ChatColor.GREEN + "③もう一方の対角線上の角を" + ChatColor.RED + "右" + ChatColor.GREEN + "クリック\n"
						+ ChatColor.RESET + "" +  ChatColor.GREEN + "④メニューの" + ChatColor.RESET + "" +  ChatColor.YELLOW + "金の斧" + ChatColor.RESET + "" +  ChatColor.GREEN + "をクリック\n"
						+ ChatColor.DARK_GREEN + "解説ページ→" + ChatColor.UNDERLINE + "http://seichi.click/d/WorldGuard"
						);
			}

			else if(itemstackcurrent.getType().equals(Material.GOLD_AXE)){
				// 保護の設定
				player.closeInventory();
				Selection selection = Util.getWorldEdit().getSelection(player);
				if(!player.hasPermission("worldguard.region.claim")){
					player.sendMessage(ChatColor.RED + "このワールドでは保護を申請できません");
					return;
				}else if (selection == null) {
					player.sendMessage(ChatColor.RED + "先に木の斧で範囲を指定してからこのボタンを押してください");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
					return;
				}else if(selection.getLength() < 10||selection.getWidth() < 10){
					player.sendMessage(ChatColor.RED + "指定された範囲が狭すぎます。1辺当たり最低10ブロック以上にしてください");
					player.sendMessage(ChatColor.DARK_GRAY + "[TIPS]どうしても小さい保護が必要な人は直接コマンド入力で作ろう！");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
					return;
				}

				player.chat("//expand vert");
				player.chat("/rg claim " + player.getName() + "_" + playerdata.rgnum);
				playerdata.rgnum += 1;
				player.chat("//sel");
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
			}

			else if(itemstackcurrent.getType().equals(Material.STONE_AXE)){
				// 保護リストの表示
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
				player.sendMessage(ChatColor.GRAY + "--------------------\n"
						+ ChatColor.GRAY + "複数ページの場合… " + ChatColor.RESET + "" +  ChatColor.RED + "" + ChatColor.BOLD + "/rg list ページNo\n"
						+ ChatColor.RESET + "" +  ChatColor.GRAY + "先頭に[+]のついた保護はOwner権限\n[-]のついた保護はMember権限を保有しています\n"
						+ ChatColor.DARK_GREEN + "解説ページ→" + ChatColor.UNDERLINE + "http://seichi.click/d/WorldGuard");
				player.chat("/rg list");
			}

			else if(itemstackcurrent.getType().equals(Material.DIAMOND_AXE)){
				// ReguionGUI表示
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
				player.chat("/land");
			}

			else if(itemstackcurrent.getType().equals(Material.NETHER_STAR)){
				// hubコマンド実行
				// player.chat("/hub");
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.sendMessage(ChatColor.RESET + "" +  ChatColor.GRAY + "/hubと入力してEnterを押してください");
			}

			else if(itemstackcurrent.getType().equals(Material.BOOK)){
				// wikiリンク表示
				player.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + "http://seichi.click");
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
			}

			else if(itemstackcurrent.getType().equals(Material.BOOK_AND_QUILL)){
				// 投票リンク表示
				player.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + "https://minecraft.jp/servers/play.seichi.click/vote");
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
			}

			else if(itemstackcurrent.getType().equals(Material.PAPER)){
				// 運営方針とルールリンク表示
				player.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + "http://seichi.click/d/%b1%bf%b1%c4%ca%fd%bf%cb%a4%c8%a5%eb%a1%bc%a5%eb");
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
			}

			else if(itemstackcurrent.getType().equals(Material.MAP)){
				// 鯖マップリンク表示
				player.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + "http://seichi.click/d/DynmapLinks");
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();

			}

			else if(itemstackcurrent.getType().equals(Material.SIGN)){
				// 掲示板リンク表示
				player.sendMessage(ChatColor.DARK_GRAY + "開いたら下の方までスクロールしてください\n"
						+ ChatColor.RED + "" + ChatColor.UNDERLINE + "https://minecraft.jp/servers/play.seichi.click"
						);
				player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
				player.closeInventory();
			}

			else if(itemstackcurrent.getType().equals(Material.ENDER_PORTAL_FRAME)){
				//ver0.3.2 四次元ポケットを開く
				//レベルが足りない場合処理終了
				if( playerdata.level < SeichiAssist.config.getPassivePortalInventorylevel()){
					player.sendMessage(ChatColor.GREEN + "4次元ポケットを開くには整地レベルが"+SeichiAssist.config.getPassivePortalInventorylevel()+ "以上必要です");
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					return;
				}
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_OPEN, 1, (float) 0.1);

				//レベルに応じたポケットサイズ変更処理
				//アイテム消失を防ぐ為、現在のサイズよりも四次元ポケットサイズが大きくなる場合のみ拡張処理する
				if(playerdata.inventory.getSize() < playerdata.getPocketSize()){
					//現在の四次元ポケットの中身を取得
					ItemStack[] item = playerdata.inventory.getContents();
					//新しいサイズの四次元ポケットを作成
					Inventory newsizepocket = Bukkit.getServer().createInventory(null,playerdata.getPocketSize(),ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "4次元ポケット");
					//for文で一個ずつ新しいサイズのポケットに入れてく
					int i = 0;
		            for (ItemStack m : item) {
		                newsizepocket.setItem(i, m);
		                i++;
		            }
		            //出来たら置き換える
		            playerdata.inventory = newsizepocket;
				}
				//インベントリを開く
				player.openInventory(playerdata.inventory);
			}


			else if(itemstackcurrent.getType().equals(Material.ENDER_CHEST)){
				//レベルが足りない場合処理終了
				if( playerdata.level < SeichiAssist.config.getDokodemoEnderlevel()){
					player.sendMessage(ChatColor.GREEN + "整地レベルが"+SeichiAssist.config.getDokodemoEnderlevel()+ "以上必要です");
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					return;
				}
				//どこでもエンダーチェストを開く
				player.playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_OPEN, 1, (float) 1.0);
				player.openInventory(player.getEnderChest());
			}


			else if(itemstackcurrent.getType().equals(Material.BUCKET)){
				//ゴミ箱を開く
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, (float) 1.5);
				//インベントリを開く
				player.openInventory(SeichiAssist.plugin.getServer().createInventory(null, 9*4 ,ChatColor.RED + "" + ChatColor.BOLD + "ゴミ箱(取扱注意)"));
			}


			else if(itemstackcurrent.getType().equals(Material.NOTE_BLOCK)){
				//ガチャ景品交換システムを開く
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, (float) 0.5);
				//インベントリを開く
				player.openInventory(SeichiAssist.plugin.getServer().createInventory(null, 9*4 ,ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "交換したい景品を入れてください"));
			}

		}
	}
	//スキルメニューの処理
	@EventHandler
	public void onPlayerClickActiveSkillSellectEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}


		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);

		//経験値変更用のクラスを設定
		ExperienceManager expman = new ExperienceManager(player);


		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "整地スキル選択")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			int type = 0;
			String name = null;
			int skilllevel;
			//ARROWSKILL
			type = ActiveSkill.ARROW.gettypenum();
			for(skilllevel = 4;skilllevel <= 9 ; skilllevel++){
				name = ActiveSkill.ARROW.getName(skilllevel);
				if(itemstackcurrent.getType().equals(ActiveSkill.ARROW.getMaterial(skilllevel))){
					PotionMeta potionmeta =(PotionMeta)itemstackcurrent.getItemMeta();
					if(potionmeta.getBasePotionData().getType().equals(ActiveSkill.ARROW.getPotionType(skilllevel))){
						if(playerdata.activeskilldata.skilltype == type
								&& playerdata.activeskilldata.skillnum == skilllevel){
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
							player.sendMessage(ChatColor.YELLOW + "既に選択されています");
						}else{
							playerdata.activeskilldata.updataSkill(player,type,skilllevel,1);
							player.sendMessage(ChatColor.GREEN + "アクティブスキル:" + name + "  が選択されました");
							player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
						}
					}
				}
			}
			//MULTISKILL
			type = ActiveSkill.MULTI.gettypenum();
			for(skilllevel = 4;skilllevel <= 9 ; skilllevel++){
				name = ActiveSkill.MULTI.getName(skilllevel);
				if(itemstackcurrent.getType().equals(ActiveSkill.MULTI.getMaterial(skilllevel))){
					if(playerdata.activeskilldata.skilltype == type
							&& playerdata.activeskilldata.skillnum == skilllevel){
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
						player.sendMessage(ChatColor.YELLOW + "既に選択されています");
					}else{
						playerdata.activeskilldata.updataSkill(player,type,skilllevel,1);
						player.sendMessage(ChatColor.GREEN + "アクティブスキル:" + name + "  が選択されました");
						player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
					}
				}
			}
			//BREAKSKILL
			type = ActiveSkill.BREAK.gettypenum();
			for(skilllevel = 1;skilllevel <= 9 ; skilllevel++){
				name = ActiveSkill.BREAK.getName(skilllevel);
				if(itemstackcurrent.getType().equals(ActiveSkill.BREAK.getMaterial(skilllevel))){
					if(playerdata.activeskilldata.skilltype == ActiveSkill.BREAK.gettypenum()
							&& playerdata.activeskilldata.skillnum == skilllevel){
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
						player.sendMessage(ChatColor.YELLOW + "既に選択されています");
					}else{
						playerdata.activeskilldata.updataSkill(player,type,skilllevel,1);
						player.sendMessage(ChatColor.GREEN + "アクティブスキル:" + name + "  が選択されました");
						player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
					}
				}
			}
			//CONDENSKILL
			type = ActiveSkill.CONDENSE.gettypenum();
			for(skilllevel = 4;skilllevel <= 9 ; skilllevel++){
				name = ActiveSkill.CONDENSE.getName(skilllevel);
				if(itemstackcurrent.getType().equals(ActiveSkill.CONDENSE.getMaterial(skilllevel))){
					if(playerdata.activeskilldata.assaulttype == type
							&& playerdata.activeskilldata.assaultnum == skilllevel){
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
						player.sendMessage(ChatColor.YELLOW + "既に選択されています");
					}else{
						playerdata.activeskilldata.updataAssaultSkill(player,type,skilllevel,1);
						player.sendMessage(ChatColor.DARK_GREEN + "アサルトスキル:" + name + "  が選択されました");
						player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
					}
				}
			}

			//アサルトアーマー
			type = ActiveSkill.ARMOR.gettypenum();
			skilllevel = 10;
			if(itemstackcurrent.getType().equals(ActiveSkill.ARMOR.getMaterial(skilllevel))){
				if(playerdata.activeskilldata.assaultnum == skilllevel || playerdata.activeskilldata.assaulttype == type){
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					player.sendMessage(ChatColor.YELLOW + "既に選択されています");
				}else{
					playerdata.activeskilldata.updataAssaultSkill(player,type,skilllevel,1);
					player.sendMessage(ChatColor.DARK_GREEN + "アサルトスキル:" + "アサルト・アーマー" + "  が選択されました");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
				}
			}

			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMenuData(player));
				return;
			}
			else if(itemstackcurrent.getType().equals(Material.STONE_BUTTON)){
				if(itemstackcurrent.getItemMeta().getDisplayName().contains("リセット")){
					//経験値変更用のクラスを設定
					//経験値が足りなかったら処理を終了
					if(!expman.hasExp(10000)){
						player.sendMessage(ChatColor.RED + "必要な経験値が足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
						return;
					}
					//経験値消費
					expman.changeExp(-10000);

					//リセット処理
					playerdata.activeskilldata.reset();
					//スキルポイント更新
					playerdata.activeskilldata.updataActiveSkillPoint(player, playerdata.level);
					//リセット音を流す
					player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1, (float) 0.1);
					//メッセージを流す
					player.sendMessage(ChatColor.LIGHT_PURPLE + "アクティブスキルポイントをリセットしました");
					//メニューを開く
					player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
				}
			}
			else if(itemstackcurrent.getType().equals(Material.GLASS)){
				if(playerdata.activeskilldata.skilltype == 0 && playerdata.activeskilldata.skillnum == 0
				&&playerdata.activeskilldata.assaulttype == 0 && playerdata.activeskilldata.assaultnum == 0
						){
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					player.sendMessage(ChatColor.YELLOW + "既に全ての選択は削除されています");
				}else{
					playerdata.activeskilldata.clearSellect(player);

				}
			}
			else if(itemstackcurrent.getType().equals(Material.BOOKSHELF)){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1, (float) 0.5);
				player.openInventory(MenuInventoryData.getActiveSkillEffectMenuData(player));
				return;
			}
		}
	}
	//スキルエフェクトメニューの処理 + エフェクト開放の処理
	@EventHandler
	public void onPlayerClickActiveSkillEffectSellectEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}
		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズ終了
		if(topinventory.getSize() != 9 * 6){
			return;
		}
		Player player = (Player)he;
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);

		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "整地スキルエフェクト選択")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
				return;
			}else if(itemstackcurrent.getType().equals(Material.GLASS)){
				if(playerdata.activeskilldata.effectnum == 0){
					player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					player.sendMessage(ChatColor.YELLOW + "既に選択されています");
				}else{
					playerdata.activeskilldata.effectnum = 0;
					player.sendMessage(ChatColor.GREEN + "エフェクト:未設定  が選択されました");
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
				}
				return;
			}else if(itemstackcurrent.getType().equals(Material.BOOK_AND_QUILL)){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getBuyRecordMenuData(player));
				return;
			}else{
				ActiveSkillEffect[] skilleffect = ActiveSkillEffect.values();
				for(int i = 0; i < skilleffect.length ; i++){
					if(itemstackcurrent.getType().equals(skilleffect[i].getMaterial())){
						if(playerdata.activeskilldata.effectnum == skilleffect[i].getNum()){
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
							player.sendMessage(ChatColor.YELLOW + "既に選択されています");
						}else{
							playerdata.activeskilldata.effectnum = skilleffect[i].getNum();
							player.sendMessage(ChatColor.GREEN + "エフェクト:" + skilleffect[i].getName() + ChatColor.RESET + "" + ChatColor.GREEN + " が選択されました");
							player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
						}
					}
				}
				ActiveSkillPremiumEffect[] premiumeffect = ActiveSkillPremiumEffect.values();
				for(int i = 0; i < premiumeffect.length ; i++){
					if(itemstackcurrent.getType().equals(premiumeffect[i].getMaterial())){
						if(playerdata.activeskilldata.effectnum == premiumeffect[i].getNum()){
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
							player.sendMessage(ChatColor.YELLOW + "既に選択されています");
						}else{
							playerdata.activeskilldata.effectnum = premiumeffect[i].getNum() + 100;
							player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "プレミアムエフェクト:" + premiumeffect[i].getName() + ChatColor.RESET + "" + ChatColor.GREEN + "" + ChatColor.BOLD + " が選択されました");
							player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float) 0.1);
						}
					}
				}
			}


			//ここからエフェクト開放の処理
			if(itemstackcurrent.getType().equals(Material.BEDROCK)){
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				ActiveSkillEffect[] skilleffect = ActiveSkillEffect.values();
				for(int i = 0; i < skilleffect.length ; i++){
					if(itemmeta.getDisplayName().contains(skilleffect[i].getName())){
						if(playerdata.activeskilldata.effectpoint < skilleffect[i].getUsePoint()){
							player.sendMessage(ChatColor.DARK_RED  + "エフェクトポイントが足りません");
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float)0.5);
						}else{
							skilleffect[i].setObtained(playerdata.activeskilldata.effectflagmap);
							player.sendMessage(ChatColor.LIGHT_PURPLE + "エフェクト：" + skilleffect[i].getName() + ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE+ "" + ChatColor.BOLD + "" + " を解除しました");
							player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
							playerdata.activeskilldata.effectpoint -= skilleffect[i].getUsePoint();
							player.openInventory(MenuInventoryData.getActiveSkillEffectMenuData(player));
						}
					}
				}
			}
			//ここからプレミアムエフェクト開放の処理
			if(itemstackcurrent.getType().equals(Material.BEDROCK)){
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				ActiveSkillPremiumEffect[] premiumeffect = ActiveSkillPremiumEffect.values();
				for(int i = 0; i < premiumeffect.length ; i++){
					if(itemmeta.getDisplayName().contains(premiumeffect[i].getName())){
						if(playerdata.activeskilldata.premiumeffectpoint < premiumeffect[i].getUsePoint()){
							player.sendMessage(ChatColor.DARK_RED  + "プレミアムエフェクトポイントが足りません");
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float)0.5);
						}else{
							premiumeffect[i].setObtained(playerdata.activeskilldata.premiumeffectflagmap);
							player.sendMessage(ChatColor.LIGHT_PURPLE+ "" + ChatColor.BOLD + "プレミアムエフェクト：" + premiumeffect[i].getName() + ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE+ "" + ChatColor.BOLD + "" + " を解除しました");
							if(!sql.addPremiumEffectBuy(playerdata,premiumeffect[i])){
								player.sendMessage("購入履歴が正しく記録されませんでした。管理者に報告してください。");
							}
							player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
							playerdata.activeskilldata.premiumeffectpoint -= premiumeffect[i].getUsePoint();
							player.openInventory(MenuInventoryData.getActiveSkillEffectMenuData(player));
						}
					}
				}
			}
		}
	}

	//スキル解放の処理
	@EventHandler
	public void onPlayerClickActiveSkillReleaseEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);

		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "整地スキル選択")){
			event.setCancelled(true);
			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}
			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			if(itemstackcurrent.getType().equals(Material.BEDROCK)){
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				int skilllevel = 0;
				int skilltype = 0;
				if(itemmeta.getDisplayName().contains("エビフライ・ドライブ")){
					skilllevel = 4;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float)0.5);
					}else if(playerdata.activeskilldata.breakskill < 3){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(3,3) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float)0.5);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ホーリー・ショット")){
					skilllevel = 5;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.arrowskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ツァーリ・ボンバ")){
					skilllevel = 6;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.arrowskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("アーク・ブラスト")){
					skilllevel = 7;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.arrowskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ファンタズム・レイ")){
					skilllevel = 8;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.arrowskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("スーパー・ノヴァ")){
					skilllevel = 9;
					skilltype = 1;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.arrowskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.arrowskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						if(playerdata.activeskilldata.multiskill == 9 && playerdata.activeskilldata.breakskill == 9 && playerdata.activeskilldata.condenskill == 9){
							player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "全てのスキルを習得し、アサルト・アーマーを解除しました");
							Util.sendEverySound(Sound.ENTITY_ENDERDRAGON_DEATH, 1, (float)1.2);
							Util.sendEveryMessage(ChatColor.GOLD + "" + ChatColor.BOLD + playerdata.name + "が全てのスキルを習得し、アサルトアーマーを解除しました！");
						}
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("トム・ボウイ")){
					skilllevel = 4;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < 3){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(3,3) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("サンダー・ストーム")){
					skilllevel = 5;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.multiskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("スターライト・ブレイカー")){
					skilllevel = 6;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.multiskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("アース・ディバイド")){
					skilllevel = 7;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.multiskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ヘヴン・ゲイボルグ")){
					skilllevel = 8;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.multiskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ディシジョン")){
					skilllevel = 9;
					skilltype = 2;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.multiskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.multiskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						if(playerdata.activeskilldata.arrowskill == 9 && playerdata.activeskilldata.breakskill == 9 && playerdata.activeskilldata.condenskill == 9){
							player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "全てのスキルを習得し、アサルト・アーマーを解除しました");
							Util.sendEverySound(Sound.ENTITY_ENDERDRAGON_DEATH, 1, (float)1.2);
							Util.sendEveryMessage(ChatColor.GOLD + "" + ChatColor.BOLD + playerdata.name + "が全てのスキルを習得し、アサルトアーマーを解除しました！");
						}
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("デュアル・ブレイク")){
					skilllevel = 1;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("トリアル・ブレイク")){
					skilllevel = 2;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("エクスプロージョン")){
					skilllevel = 3;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ミラージュ・フレア")){
					skilllevel = 4;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ドッ・カーン")){
					skilllevel = 5;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ギガンティック・ボム")){
					skilllevel = 6;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ブリリアント・デトネーション")){
					skilllevel = 7;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("レムリア・インパクト")){
					skilllevel = 8;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("エターナル・ヴァイス")){
					skilllevel = 9;
					skilltype = 3;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.breakskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						if(playerdata.activeskilldata.arrowskill == 9 && playerdata.activeskilldata.multiskill == 9 && playerdata.activeskilldata.condenskill == 9){
							player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "全てのスキルを習得し、アサルト・アーマーを解除しました");
							Util.sendEverySound(Sound.ENTITY_ENDERDRAGON_DEATH, 1, (float)1.2);
							Util.sendEveryMessage(ChatColor.GOLD + "" + ChatColor.BOLD + playerdata.name + "が全てのスキルを習得し、アサルトアーマーを解除しました！");
						}
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ホワイト・ブレス")){
					skilllevel = 4;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.breakskill < 3){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(3,3) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("アブソリュート・ゼロ")){
					skilllevel = 5;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.condenskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ダイアモンド・ダスト")){
					skilllevel = 6;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.condenskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("ラヴァ・コンデンセーション")){
					skilllevel = 7;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.condenskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("モエラキ・ボールダーズ")){
					skilllevel = 8;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.condenskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("エルト・フェットル")){
					skilllevel = 9;
					skilltype = 4;
					if(playerdata.activeskilldata.skillpoint < skilllevel * 10){
						player.sendMessage(ChatColor.DARK_RED  + "アクティブスキルポイントが足りません");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else if(playerdata.activeskilldata.condenskill < skilllevel - 1){
						player.sendMessage(ChatColor.DARK_RED + "前提スキル[" + ActiveSkill.getActiveSkillName(skilltype,skilllevel - 1) + "]を習得する必要があります");
						player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1, (float) 0.1);
					}else{
						playerdata.activeskilldata.condenskill = skilllevel;
						player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "" + ActiveSkill.getActiveSkillName(skilltype ,skilllevel) + "を解除しました");
						player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float)1.2);
						playerdata.activeskilldata.updataActiveSkillPoint(player,playerdata.level);
						if(playerdata.activeskilldata.arrowskill == 9 && playerdata.activeskilldata.breakskill == 9 && playerdata.activeskilldata.multiskill == 9){
							player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "全てのスキルを習得し、アサルト・アーマーを解除しました");
							Util.sendEverySound(Sound.ENTITY_ENDERDRAGON_DEATH, 1, (float)1.2);
							Util.sendEveryMessage(ChatColor.GOLD + "" + ChatColor.BOLD + playerdata.name + "が全てのスキルを習得し、アサルトアーマーを解除しました！");
						}
						player.openInventory(MenuInventoryData.getActiveSkillMenuData(player));
					}
				}else if(itemmeta.getDisplayName().contains("アサルト・アーマー")){

				}


			}
		}
	}
/*
	//共有マインスタックメニュー
	@EventHandler
	public void onPlayerClickMultiMineStackMenuEvent(InventoryClickEvent event){
		//枠外のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーじゃないとき終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventry = view.getTopInventory();
		//インベントリが存在しないとき終了
	}
	*/

	//マインスタックメニュー
	@EventHandler
	public void onPlayerClickMineStackMenuEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);

		//インベントリ名が以下の時処理

		if(topinventory.getTitle().equals(ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "MineStack")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMenuData(player));
				return;
			}

			if(itemstackcurrent.getType().equals(Material.IRON_PICKAXE)){
				// 対象ブロック自動スタック機能トグル
				playerdata.minestackflag = !playerdata.minestackflag;
				if(playerdata.minestackflag){
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
					player.sendMessage(ChatColor.GREEN + "対象ブロック自動スタック機能:ON");
				}else{
					player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
					player.sendMessage(ChatColor.RED + "対象ブロック自動スタック機能:OFF");
				}
				ItemMeta itemmeta = itemstackcurrent.getItemMeta();
				itemstackcurrent.setItemMeta(MenuInventoryData.MineStackToggleMeta(playerdata,itemmeta));
			}

			//dirt
			else if(itemstackcurrent.getType().equals(Material.DIRT)){
				playerdata.minestack.dirt = giveMineStack(player,playerdata.minestack.dirt,Material.DIRT);
			}

			//grass
			else if(itemstackcurrent.getType().equals(Material.GRASS)){
				playerdata.minestack.grass = giveMineStack(player,playerdata.minestack.grass,Material.GRASS);
			}

			//gravel
			else if(itemstackcurrent.getType().equals(Material.GRAVEL)){
				playerdata.minestack.gravel = giveMineStack(player,playerdata.minestack.gravel,Material.GRAVEL);
			}

			//cobblestone
			else if(itemstackcurrent.getType().equals(Material.COBBLESTONE)){
				playerdata.minestack.cobblestone = giveMineStack(player,playerdata.minestack.cobblestone,Material.COBBLESTONE);
			}

			//stone
			else if(itemstackcurrent.getType().equals(Material.STONE)){
				playerdata.minestack.stone = giveMineStack(player,playerdata.minestack.stone,Material.STONE);
			}

			//sand
			else if(itemstackcurrent.getType().equals(Material.SAND)){
				playerdata.minestack.sand = giveMineStack(player,playerdata.minestack.sand,Material.SAND);
			}
			//packed_ice
			else if(itemstackcurrent.getType().equals(Material.PACKED_ICE)){
				playerdata.minestack.packed_ice = giveMineStack(player,playerdata.minestack.packed_ice,Material.PACKED_ICE);
			}
			//sandstone
			else if(itemstackcurrent.getType().equals(Material.SANDSTONE)){
				playerdata.minestack.sandstone = giveMineStack(player,playerdata.minestack.sandstone,Material.SANDSTONE);
			}

			//netherrack
			else if(itemstackcurrent.getType().equals(Material.NETHERRACK)){
				playerdata.minestack.netherrack = giveMineStack(player,playerdata.minestack.netherrack,Material.NETHERRACK);
			}

			//soul_sand
			else if(itemstackcurrent.getType().equals(Material.SOUL_SAND)){
				playerdata.minestack.soul_sand = giveMineStack(player,playerdata.minestack.soul_sand,Material.SOUL_SAND);
			}

			//magma
			else if(itemstackcurrent.getType().equals(Material.MAGMA)){
				playerdata.minestack.magma = giveMineStack(player,playerdata.minestack.magma,Material.MAGMA);
			}

			//ender_stone
			else if(itemstackcurrent.getType().equals(Material.ENDER_STONE)){
				playerdata.minestack.ender_stone = giveMineStack(player,playerdata.minestack.ender_stone,Material.ENDER_STONE);
			}

			//coal
			else if(itemstackcurrent.getType().equals(Material.COAL)){
				playerdata.minestack.coal = giveMineStack(player,playerdata.minestack.coal,Material.COAL);
			}

			//coal_ore
			else if(itemstackcurrent.getType().equals(Material.COAL_ORE)){
				playerdata.minestack.coal_ore = giveMineStack(player,playerdata.minestack.coal_ore,Material.COAL_ORE);
			}

			//iron_ore
			else if(itemstackcurrent.getType().equals(Material.IRON_ORE)){
				playerdata.minestack.iron_ore = giveMineStack(player,playerdata.minestack.iron_ore,Material.IRON_ORE);
			}

			//quartz
			else if(itemstackcurrent.getType().equals(Material.QUARTZ)){
				playerdata.minestack.quartz = giveMineStack(player,playerdata.minestack.quartz,Material.QUARTZ);
			}

			//quartz_ore
			else if(itemstackcurrent.getType().equals(Material.QUARTZ_ORE)){
				playerdata.minestack.quartz_ore = giveMineStack(player,playerdata.minestack.quartz_ore,Material.QUARTZ_ORE);
			}

			player.openInventory(MenuInventoryData.getMineStackMenu(player));

		}
	}

	//ランキングメニュー
	@EventHandler
	public void onPlayerClickSeichiRankingMenuEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;

		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "整地神ランキング")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_OPEN, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getMenuData(player));
				return;
			}
		}
	}
	//購入履歴メニュー
	@EventHandler
	public void onPlayerClickPremiumLogMenuEvent(InventoryClickEvent event){
		//外枠のクリック処理なら終了
		if(event.getClickedInventory() == null){
			return;
		}

		ItemStack itemstackcurrent = event.getCurrentItem();
		InventoryView view = event.getView();
		HumanEntity he = view.getPlayer();
		//インベントリを開けたのがプレイヤーではない時終了
		if(!he.getType().equals(EntityType.PLAYER)){
			return;
		}

		Inventory topinventory = view.getTopInventory();
		//インベントリが存在しない時終了
		if(topinventory == null){
			return;
		}
		//インベントリサイズが36でない時終了
		if(topinventory.getSize() != 36){
			return;
		}
		Player player = (Player)he;

		//インベントリ名が以下の時処理
		if(topinventory.getTitle().equals(ChatColor.BLUE + "" + ChatColor.BOLD + "プレミアムエフェクト購入履歴")){
			event.setCancelled(true);

			//プレイヤーインベントリのクリックの場合終了
			if(event.getClickedInventory().getType().equals(InventoryType.PLAYER)){
				return;
			}

			/*
			 * クリックしたボタンに応じた各処理内容の記述ここから
			 */
			//ページ変更処理
			if(itemstackcurrent.getType().equals(Material.SKULL_ITEM) && ((SkullMeta)itemstackcurrent.getItemMeta()).getOwner().equals("MHF_ArrowLeft")){
				//開く音を再生
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, (float) 0.1);
				player.openInventory(MenuInventoryData.getActiveSkillEffectMenuData(player));
				return;
			}
		}
	}
	//minestackの1stack付与の処理
	private int giveMineStack(Player player,int minestack,Material type){
		if(minestack >= 64){
			ItemStack itemstack = new ItemStack(type,64);
			if(!Util.isPlayerInventryFill(player)){
				Util.addItem(player,itemstack);
			}else{
				Util.dropItem(player,itemstack);
			}
			minestack -= 64;
			player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 1);
		}else if(minestack == 0){
			return minestack;
		}else{
			ItemStack itemstack = new ItemStack(type,minestack);
			if(!Util.isPlayerInventryFill(player)){
				Util.addItem(player,itemstack);
			}else{
				Util.dropItem(player,itemstack);
			}
			minestack -= minestack;
			player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, (float)0.5);
		}
		return minestack;
	}


    //ガチャ交換システム
    @EventHandler
    public void onGachaTradeEvent(InventoryCloseEvent event){
        Player player = (Player)event.getPlayer();
		UUID uuid = player.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);
		//エラー分岐
		if(playerdata == null){
			return;
		}
		String name = playerdata.name;
        Inventory inventory = event.getInventory();

        //インベントリサイズが36でない時終了
        if(inventory.getSize() != 36){
            return;
        }
        if(inventory.getTitle().equals(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "交換したい景品を入れてください")){
            //PlayerInventory pinventory = player.getInventory();
            //ItemStack itemstack = pinventory.getItemInMainHand();
            int givegacha = 0;
            /*この分岐処理必要かなぁ…とりあえずコメントアウト
            if(itemstack.getType().equals(Material.STICK)){
            }
            */
            /*
             * step1 for文でinventory内に対象商品がないか検索
             * あったらdurabilityに応じてgivegachaを増やし、非対象商品は返却boxへ
             */
            //ガチャ景品交換インベントリの中身を取得
            ItemStack[] item = inventory.getContents();
            //ドロップ用アイテムリスト(返却box)作成
            List<ItemStack> dropitem = new ArrayList<ItemStack>();
            //カウント用
            int big = 0;
            int reg = 0;
            //for文で１個ずつ対象アイテムか見る
            //ガチャ景品交換インベントリを一個ずつ見ていくfor文
            for (ItemStack m : item) {
                //無いなら次へ
                if(m == null){
                    continue;
                }else if(SeichiAssist.gachamente){
                    //ガチャシステムメンテナンス中は全て返却する
                    dropitem.add(m);
                    continue;
                }else if(!m.hasItemMeta()){
                    //丁重にお返しする
                    dropitem.add(m);
                    continue;
                }else if(!m.getItemMeta().hasLore()){
                    //丁重にお返しする
                    dropitem.add(m);
                    continue;
                }else if(m.getType().equals(Material.SKULL_ITEM)){
                    //丁重にお返しする
                    dropitem.add(m);
                    continue;
                }
                //ガチャ景品リストにアイテムがあった時にtrueになるフラグ
                boolean flag = false;
                //ガチャ景品リストを一個ずつ見ていくfor文
                for(GachaData gachadata : gachadatalist){
                    if(!gachadata.itemstack.hasItemMeta()){
                        continue;
                    }else if(!gachadata.itemstack.getItemMeta().hasLore()){
                        continue;
                    }
                    //ガチャ景品リストにある商品の場合(Lore=説明文と表示名で判別),無い場合はアイテム返却
                    if(gachadata.compare(m,name)){
                    	if(SeichiAssist.DEBUG){
                    		player.sendMessage(gachadata.itemstack.getItemMeta().getDisplayName());
                    	}
                    //if(gachadata.itemstack.getItemMeta().getLore().equals(m.getItemMeta().getLore())
                           // &&gachadata.itemstack.getItemMeta().getDisplayName().equals(m.getItemMeta().getDisplayName())){
                        flag = true;
                        int amount = m.getAmount();
                        if(gachadata.probability < 0.001){
                            //ギガンティック大当たりの部分
                            //ガチャ券に交換せずそのままアイテムを返す
                            dropitem.add(m);
                        }else if(gachadata.probability < 0.01){
                            //大当たりの部分
                            givegacha += (12*amount);
                            big++;
                        }else if(gachadata.probability < 0.1){
                            //当たりの部分
                            givegacha += (3*amount);
                            reg++;
                        }else{
                            //それ以外アイテム返却(経験値ポーションとかがここにくるはず)
                            dropitem.add(m);
                        }
                        break;
                    }
                }
                //ガチャ景品リストに対象アイテムが無かった場合
                if(!flag){
                    //丁重にお返しする
                    dropitem.add(m);
                }
            }
            //ガチャシステムメンテナンス中は全て返却する
            if(SeichiAssist.gachamente){
                player.sendMessage(ChatColor.RED + "ガチャシステムメンテナンス中の為全てのアイテムを返却します");
            }else if(!(big > 0)&&!(reg > 0)){
                player.sendMessage(ChatColor.YELLOW + "景品を認識しませんでした。全てのアイテムを返却します");
            }else{
                player.sendMessage(ChatColor.GREEN + "大当たり景品を" + big + "個、当たり景品を" + reg + "個認識しました");
            }
            /*
             * step2 非対象商品をインベントリに戻す
             */
            for(ItemStack m : dropitem){
                if(!Util.isPlayerInventryFill(player)){
                    Util.addItem(player,m);
                }else{
                    Util.dropItem(player,m);
                }
            }
            /*
             * step3 ガチャ券をインベントリへ
             */
            ItemStack skull = Util.getExchangeskull(Util.getName(player));
            int count = 0;
            while(givegacha > 0){
                if(player.getInventory().contains(skull) || !Util.isPlayerInventryFill(player)){
                    Util.addItem(player,skull);
                }else{
                    Util.dropItem(player,skull);
                }
                givegacha--;
                count++;
            }
            if(count > 0){
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
                player.sendMessage(ChatColor.GREEN + ""+count+ "枚の" + ChatColor.GOLD + "ガチャ券" + ChatColor.WHITE + "を受け取りました");
            }
        }
    }
}