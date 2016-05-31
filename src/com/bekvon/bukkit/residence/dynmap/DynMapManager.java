package com.bekvon.bukkit.residence.dynmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.economy.TransactionManager;
import com.bekvon.bukkit.residence.economy.rent.RentManager;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.bekvon.bukkit.residence.utils.GetTime;

public class DynMapManager {
    Residence plugin;

    public DynmapAPI api;
    MarkerAPI markerapi;
    MarkerSet set;
    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();
    private int schedId = -1;

    public DynMapManager(Residence plugin) {
	this.plugin = plugin;
    }

    public MarkerSet getMarkerSet() {
	return set;
    }

    public void fireUpdateAdd(final ClaimedResidence res, final int deep) {
	if (api == null || set == null)
	    return;
	if (res == null)
	    return;

	if (schedId != -1)
	    Bukkit.getServer().getScheduler().cancelTask(schedId);

	schedId = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
	    public void run() {
		schedId = -1;

		handleResidenceAdd(res.getName(), res, resareas, deep);
		return;
	    }
	}, 10L);
    }

    public void fireUpdateRemove(final ClaimedResidence res, final int deep) {
	if (api == null || set == null)
	    return;
	if (res == null)
	    return;

	handleResidenceRemove(res.getName(), res, resareas, deep);
    }

    private String formatInfoWindow(String resid, ClaimedResidence res, String resName) {
	if (res == null)
	    return null;
	if (res.getName() == null)
	    return null;
	if (res.getOwner() == null)
	    return null;
	String v =
	    "<div class=\"regioninfo\"><div class=\"infowindow\"><span style=\"font-size:140%;font-weight:bold;\">%regionname%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("General.Owner", "")) + "<span style=\"font-weight:bold;\">%playerowners%</span><br />";

	if (Residence.getConfigManager().DynMapShowFlags)
	    v += ChatColor.stripColor(Residence.getLM().getMessage("General.Flags", "")) + "<br /><span style=\"font-weight:bold;\">%flags%</span>";
	v += "</div></div>";

	if (Residence.getRentManager().isForRent(res.getName()))
	    v = "<div class=\"regioninfo\"><div class=\"infowindow\">"
		+ ChatColor.stripColor(Residence.getLM().getMessage("Rentable.Land", "")) + "<span style=\"font-size:140%;font-weight:bold;\">%regionname%</span><br />"
		+ ChatColor.stripColor(Residence.getLM().getMessage("General.Owner", "")) + "<span style=\"font-weight:bold;\">%playerowners%</span><br />"
		+ ChatColor.stripColor(Residence.getLM().getMessage("Residence.RentedBy", "")) + "<span style=\"font-weight:bold;\">%renter%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("General.LandCost", "")) + "<span style=\"font-weight:bold;\">%rent%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("Rent.Days", "")) + "<span style=\"font-weight:bold;\">%rentdays%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("Rentable.AllowRenewing", "")) + "<span style=\"font-weight:bold;\">%renew%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("Rent.Expire", "")) + "<span style=\"font-weight:bold;\">%expire%</span></div></div>";

	if (Residence.getTransactionManager().isForSale(res.getName()))
	    v = "<div class=\"regioninfo\"><div class=\"infowindow\">"
		+ ChatColor.stripColor(Residence.getLM().getMessage("Economy.LandForSale", " "))
		+ "<span style=\"font-size:140%;font-weight:bold;\">%regionname%</span><br /> "
		+ ChatColor.stripColor(Residence.getLM().getMessage("General.Owner", "")) + "<span style=\"font-weight:bold;\">%playerowners%</span><br />"
		+ ChatColor.stripColor(Residence.getLM().getMessage("Economy.SellAmount", "")) + "<span style=\"font-weight:bold;\">%price%</span><br /></div></div>";

	v = v.replace("%regionname%", resName);
	v = v.replace("%playerowners%", res.getOwner());
	String m = res.getEnterMessage();
	v = v.replace("%entermsg%", (m != null) ? m : "");
	m = res.getLeaveMessage();
	v = v.replace("%leavemsg%", (m != null) ? m : "");
	ResidencePermissions p = res.getPermissions();
	String flgs = "";

	// remake
	Map<String, Boolean> all = Residence.getPermissionManager().getAllFlags().getFlags();
	String[] FLAGS = new String[all.size()];
	int ii = 0;
	for (Entry<String, Boolean> one : all.entrySet()) {
	    FLAGS[ii] = one.getKey();
	    ii++;
	}

	for (int i = 0; i < FLAGS.length; i++) {
	    if (p.isSet(FLAGS[i])) {
		if (flgs.length() > 0)
		    flgs += "<br/>";
		boolean f = p.has(FLAGS[i], false);
		flgs += FLAGS[i] + ": " + f;
		v = v.replace("%flag." + FLAGS[i] + "%", Boolean.toString(f));
	    } else
		v = v.replace("%flag." + FLAGS[i] + "%", "");
	}
	v = v.replace("%flags%", flgs);
	RentManager rentmgr = Residence.getRentManager();
	TransactionManager transmgr = Residence.getTransactionManager();

	if (rentmgr.isForRent(res.getName())) {
	    boolean isrented = rentmgr.isRented(resid);
	    v = v.replace("%isrented%", Boolean.toString(isrented));
	    String id = "";
	    if (isrented)
		id = rentmgr.getRentingPlayer(resid);
	    v = v.replace("%renter%", id);

	    v = v.replace("%rent%", rentmgr.getCostOfRent(resid) + "");
	    v = v.replace("%rentdays%", rentmgr.getRentDays(resid) + "");
	    boolean renew = rentmgr.getRentableRepeatable(resid);
	    v = v.replace("%renew%", renew + "");
	    String expire = "";
	    if (isrented) {
		long time = rentmgr.getRentedLand(resid).endTime;
		if (time != 0L)
		    expire = GetTime.getTime(time);
	    }
	    v = v.replace("%expire%", expire);
	}

	if (transmgr.isForSale(res.getName())) {
	    boolean forsale = transmgr.isForSale(resid);
	    v = v.replace("%isforsale%", Boolean.toString(transmgr.isForSale(resid)));
	    String price = "";
	    if (forsale)
		price = Integer.toString(transmgr.getSaleAmount(resid));
	    v = v.replace("%price%", price);
	}

	return v;
    }

    private boolean isVisible(String id, String worldname) {
	List<String> visible = Residence.getConfigManager().DynMapVisibleRegions;
	List<String> hidden = Residence.getConfigManager().DynMapHiddenRegions;
	if (visible != null && visible.size() > 0) {
	    if ((visible.contains(id) == false) && (visible.contains("world:" + worldname) == false)) {
		return false;
	    }
	}
	if (hidden != null && hidden.size() > 0) {
	    if (hidden.contains(id) || hidden.contains("world:" + worldname))
		return false;
	}
	return true;
    }

    private void addStyle(String resid, AreaMarker m) {
	AreaStyle as = new AreaStyle();
	int sc = 0xFF0000;
	int fc = 0xFF0000;
	try {
	    sc = Integer.parseInt(as.strokecolor.substring(1), 16);
	    if (Residence.getRentManager().isForRent(resid) && !Residence.getRentManager().isRented(resid))
		fc = Integer.parseInt(as.forrentstrokecolor.substring(1), 16);
	    else if (Residence.getRentManager().isForRent(resid) && Residence.getRentManager().isRented(resid))
		fc = Integer.parseInt(as.rentedstrokecolor.substring(1), 16);
	    else if (Residence.getTransactionManager().isForSale(resid))
		fc = Integer.parseInt(as.forsalestrokecolor.substring(1), 16);
	    else
		fc = Integer.parseInt(as.fillcolor.substring(1), 16);
	} catch (NumberFormatException nfx) {
	}
	m.setLineStyle(as.strokeweight, as.strokeopacity, sc);
	m.setFillStyle(as.fillopacity, fc);
	m.setRangeY(as.y, as.y);
    }

    private void handleResidenceAdd(String resid, ClaimedResidence res, Map<String, AreaMarker> newmap, int depth) {

	if (res == null)
	    return;

	boolean hidden = res.getPermissions().has("hidden", false);
	if (hidden && Residence.getConfigManager().DynMapHideHidden) {
	    fireUpdateRemove(res, depth);
	    return;
	}

	for (Entry<String, CuboidArea> oneArea : res.getAreaMap().entrySet()) {

	    String id = oneArea.getKey() + "." + resid;

	    String name = res.getName();
	    double[] x = new double[2];
	    double[] z = new double[2];

	    String resName = res.getName();

	    if (res.getAreaMap().size() > 1) {
		resName = res.getName() + " (" + oneArea.getKey() + ")";
	    }

	    String desc = formatInfoWindow(resid, res, resName);

	    if (!isVisible(resid, res.getWorld()))
		return;

	    Location l0 = oneArea.getValue().getLowLoc();
	    Location l1 = oneArea.getValue().getHighLoc();

	    x[0] = l0.getX();
	    z[0] = l0.getZ();
	    x[1] = l1.getX() + 1.0;
	    z[1] = l1.getZ() + 1.0;

	    AreaMarker marker = null;

	    if (resareas.containsKey(id)) {
		marker = resareas.get(id);
		resareas.remove(id);
		marker.deleteMarker();
	    }

	    marker = set.createAreaMarker(id, name, true, res.getWorld(), x, z, false);
	    if (marker == null)
		return;

	    if (Residence.getConfigManager().DynMapLayer3dRegions)
		marker.setRangeY(Math.min(l0.getY(), l1.getY()), Math.max(l0.getY(), l1.getY()));

	    marker.setDescription(desc);
	    addStyle(resid, marker);
	    newmap.put(id, marker);

	    if (depth <= Residence.getConfigManager().DynMapLayerSubZoneDepth) {
		List<ClaimedResidence> subids = res.getSubzones();
		for (ClaimedResidence one : subids) {
		    handleResidenceAdd(one.getName(), one, newmap, depth + 1);
		}
	    }
	}
    }

    private void handleResidenceRemove(String resid, ClaimedResidence res, Map<String, AreaMarker> newmap, int depth) {

	if (res == null)
	    return;

	for (Entry<String, CuboidArea> oneArea : res.getAreaMap().entrySet()) {
	    String id = oneArea.getKey() + "." + resid;
	    if (resareas.containsKey(id)) {
		AreaMarker marker = resareas.remove(id);
		marker.deleteMarker();
	    }
	    if (depth <= Residence.getConfigManager().DynMapLayerSubZoneDepth + 1) {
		List<ClaimedResidence> subids = res.getSubzones();
		for (ClaimedResidence one : subids) {
		    handleResidenceRemove(one.getName(), one, newmap, depth + 1);
		}
	    }
	}
    }

    public void activate() {
	markerapi = api.getMarkerAPI();
	if (markerapi == null) {
	    Bukkit.getConsoleSender().sendMessage("[Residence] Error loading dynmap marker API!");
	    return;
	}

	if (set != null) {
	    set.deleteMarkerSet();
	    set = null;
	}
	set = markerapi.getMarkerSet("residence.markerset");
	if (set == null)
	    set = markerapi.createMarkerSet("residence.markerset", "Residence", null, false);
	else
	    set.setMarkerSetLabel("Residence");

	if (set == null) {
	    Bukkit.getConsoleSender().sendMessage("Error creating marker set");
	    return;
	}
	set.setLayerPriority(1);
	set.setHideByDefault(false);

	Bukkit.getConsoleSender().sendMessage("[Residence] DynMap residence activated!");

	for (Entry<String, ClaimedResidence> one : Residence.getResidenceManager().getResidences().entrySet()) {
	    Residence.getDynManager().fireUpdateAdd(one.getValue(), one.getValue().getSubzoneDeep());
	    handleResidenceAdd(one.getValue().getName(), one.getValue(), resareas, one.getValue().getSubzoneDeep());
	}
    }
}
