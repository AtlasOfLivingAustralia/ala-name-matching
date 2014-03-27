
package au.org.ala.biocache.tool

import au.org.ala.biocache._
import au.org.ala.biocache.persistence.PersistenceManager

/**
 * Rename the cassandra columns.  This is a slow process...
 *
 * TODO - improve the performance by grouping together a bunch of updates and deletes ...
 */
object RenameColumns {
  val defaultMap = Map[String, String]("ibra_merged" -> "cl20", "imcra4_pb" -> "cl21", "aus1" -> "cl22", "aus2" -> "cl23", "fal_dom_no_01" -> "cl604", "til_dom_ha_01" -> "cl605", "st_dom_no_01" -> "cl606", "til_dom_no_01" -> "cl611", "fal_dom_ha_01" -> "cl612", "irr_dom_no_01" -> "cl613", "st_dom_ha_01" -> "cl614", "native_veg" -> "cl617", "landcover" -> "cl618", "vast" -> "cl619", "present_veg" -> "cl620", "sodicity" -> "cl664", "landuse" -> "cl678", "capad08_ext" -> "cl896", "diwa_type_criteria" -> "cl901", "ger_analysis_boundary_v01" -> "cl902", "ger_border_ranges" -> "cl903", "ger_geri_boundary_v102_australia" -> "cl904", "ger_hunter" -> "cl905", "ger_hunter_analysis_mask" -> "cl906", "ger_hunter_areas_of_interest" -> "cl907", "ger_k2c_management_regions_oct2009" -> "cl908", "ger_kosciuszko_to_coast" -> "cl909", "ger_s2s_priority_area_billabong_creek_v01" -> "cl910", "ger_s2s_priority_areas_v05" -> "cl911", "ger_slopes_to_summit" -> "cl912", "ger_upper_hunter_focus_area_v2" -> "cl913", "ibra_sub_merged" -> "cl914", "ramsar" -> "cl915", "nrm_regions_2010" -> "cl916", "australian_coral_ecoregions" -> "cl917", "nrm_exp" -> "el591", "rel_spr" -> "el593", "rain_aut" -> "el594", "rel_sum" -> "el595", "rain_win" -> "el596", "erosivity" -> "el597", "rain_ann" -> "el598", "rel_win" -> "el599", "rel_aut" -> "el600", "rain_sum" -> "el601", "rain_spr" -> "el602", "aria" -> "el647", "dairy_dse" -> "el658", "sheep_dse" -> "el659", "beef_dse" -> "el660", "erodibility" -> "el661", "pawhc" -> "el662", "drain_cv" -> "el666", "soilm_cv" -> "el667", "evap_mean" -> "el668", "soilm_mean" -> "el669", "drain_mean" -> "el670", "evap_cv" -> "el671", "runoff_mean" -> "el672", "ndvi_mean" -> "el673", "elevation" -> "el674", "slope_length" -> "el675", "npp_mean" -> "el676", "aspect" -> "el680", "threatnd_sp" -> "el681", "migratry_sp" -> "el682", "wdef_mean" -> "el706", "mintx" -> "el707", "pwat_max" -> "el708", "spls_mean" -> "el709", "wpot_max" -> "el710", "rainx" -> "el711", "adefi" -> "el712", "rainm" -> "el713", "wdef_max" -> "el714", "arid_mean" -> "el715", "rtimin" -> "el716", "wpot_mean" -> "el717", "srain2mp" -> "el718", "arid_min" -> "el719", "evapi" -> "el720", "pwat_mean" -> "el721", "megagi" -> "el722", "wpot_min" -> "el723", "vpd2min" -> "el724", "trnga" -> "el725", "raini" -> "el726", "wdef_min" -> "el727", "rh2mean" -> "el728", "maxtx" -> "el729", "minti" -> "el730", "mesogi" -> "el731", "vpd29_x" -> "el732", "vpd2max" -> "el733", "slrain0" -> "el734", "vpd29_i" -> "el735", "eaeo_max" -> "el736", "mintm" -> "el737", "vpd2mean" -> "el738", "trngx" -> "el739", "adefm" -> "el740", "tminabsi" -> "el741", "rhu215_x" -> "el742", "rh2max" -> "el743", "rhu215_i" -> "el744", "srain0mp" -> "el745", "rhu215_m" -> "el746", "evapm" -> "el747", "pwat_min" -> "el748", "rh2min" -> "el749", "srain1mp" -> "el750", "c4gi" -> "el751", "tmaxabsx" -> "el752", "rtxmin" -> "el753", "rprecmin" -> "el754", "eaeo_mean" -> "el755", "rtxmax" -> "el756", "vpd29_m" -> "el757", "slrain2" -> "el758", "rtimax" -> "el759", "tmaxabsm" -> "el760", "trngm" -> "el761", "trngi" -> "el762", "adefx" -> "el763", "slrain1" -> "el764", "arid_max" -> "el765", "srain2" -> "el766", "evapx" -> "el767", "radnx" -> "el768", "maxti" -> "el769", "radni" -> "el770", "eaeo_min" -> "el771", "srain0" -> "el772", "spls_max" -> "el773", "maxtm" -> "el774", "radnm" -> "el775", "tminabsm" -> "el776", "spls_min" -> "el777", "srain1" -> "el778", "rprecmax" -> "el779", "worldclim_bio_6" -> "el781", "worldclim_bio_17" -> "el782", "worldclim_bio_4" -> "el783", "worldclim_bio_8" -> "el784", "worldclim_bio_2" -> "el785", "worldclim_bio_19" -> "el786", "worldclim_bio_7" -> "el787", "worldclim_bio_12" -> "el788", "worldclim_bio_5" -> "el789", "worldclim_bio_3" -> "el790", "worldclim_bio_14" -> "el791", "worldclim_bio_10" -> "el792", "worldclim_bio_1" -> "el793", "worldclim_bio_15" -> "el794", "worldclim_bio_11" -> "el795", "worldclim_bio_16" -> "el796", "worldclim_bio_18" -> "el797", "worldclim_bio_13" -> "el798", "worldclim_bio_9" -> "el799", "mean-oxygen_cars2006" -> "el800", "mean-silicate_cars2006" -> "el801", "mean-nitrate_cars2006" -> "el802", "mean-phosphate_cars2006" -> "el803", "mean-salinity_cars2009a" -> "el804", "mean-temperature_cars2009a" -> "el805", "substrate_bdensity" -> "el806", "substrate_geollmeanage" -> "el807", "substrate_geolmeanage" -> "el808", "substrate_geollrngeage" -> "el809", "substrate_distanywater" -> "el810", "substrate_pmnln0" -> "el811", "substrate_solpawhc" -> "el812", "substrate_datasupt" -> "el813", "substrate_clay" -> "el814", "substrate_valleybottom" -> "el815", "substrate_soldepth" -> "el816", "substrate_pmnlconcn0" -> "el817", "substrate_wr_unr" -> "el818", "substrate_distcoast" -> "el819", "substrate_distnonpermw" -> "el820", "substrate_nmnlconcn0" -> "el821", "substrate_nmnln0" -> "el822", "substrate_erosional" -> "el823", "substrate_hpedality" -> "el825", "substrate_twi" -> "el826", "substrate_relief" -> "el827", "substrate_ptotn0" -> "el828", "substrate_calcrete" -> "el829", "substrate_distpermwat" -> "el830", "substrate_ntotn0" -> "el831", "substrate_gravity" -> "el832", "substrate_ksat" -> "el833", "substrate_mrrtf" -> "el834", "substrate_geolrngeage" -> "el835", "substrate_slope" -> "el836", "substrate_corg0" -> "el837", "substrate_fert" -> "el838", "substrate_ridgetopflat" -> "el839", "substrate_mrvbf" -> "el840", "substrate_pedality" -> "el841", "substrate_ks_err" -> "el842", "substrate_nutrients" -> "el843", "substrate_roughness" -> "el844", "substrate_coarse" -> "el845", "bath_topo_ausbath_09_v4" -> "el848", "wind_windsp9m" -> "el849", "wind_windspmean" -> "el850", "wind_windspmax" -> "el851", "wind_windsp9x" -> "el852", "wind_windsp15x" -> "el853", "wind_windrx" -> "el854", "wind_windspmin" -> "el855", "wind_windsp15i" -> "el856", "wind_windri" -> "el857", "wind_windsp9i" -> "el858", "wind_windsp15m" -> "el859", "wind_windrm" -> "el860", "bioclim_bio25" -> "el861", "bioclim_bio7" -> "el862", "bioclim_bio19" -> "el863", "bioclim_bio33" -> "el864", "bioclim_bio32" -> "el865", "bioclim_bio13" -> "el866", "bioclim_bio6" -> "el867", "bioclim_bio34" -> "el868", "bioclim_bio27" -> "el869", "bioclim_bio8" -> "el870", "bioclim_bio22" -> "el871", "bioclim_bio14" -> "el872", "bioclim_bio35" -> "el873", "bioclim_bio1" -> "el874", "bioclim_bio9" -> "el875", "bioclim_bio11" -> "el876", "bioclim_bio24" -> "el877", "bioclim_bio18" -> "el878", "bioclim_bio5" -> "el879", "bioclim_bio21" -> "el880", "bioclim_bio20" -> "el881", "bioclim_bio15" -> "el882", "bioclim_bio3" -> "el883", "bioclim_bio29" -> "el884", "bioclim_bio31" -> "el885", "bioclim_bio16" -> "el886", "bioclim_bio23" -> "el887", "bioclim_bio2" -> "el888", "bioclim_bio17" -> "el889", "bioclim_bio10" -> "el890", "bioclim_bio28" -> "el891", "bioclim_bio4" -> "el892", "bioclim_bio12" -> "el893", "bioclim_bio26" -> "el894", "bioclim_bio30" -> "el895", "ALA-SPATIAL_layer_occurrence_av_4" -> "el898", "ALA-SPATIAL_layer_species_av_4" -> "el899", "east_afa_final" -> "el900")
  val persistenceManager = Config.getInstance(classOf[PersistenceManager]).asInstanceOf[PersistenceManager]

  def main(args: Array[String]): Unit = {
    if (args.length == 3)
      processMap(args(0), "", Map(args(1) -> args(2)))
    else
      processMap("loc", "", defaultMap)
  }

  def processMap(entity: String, startUuid: String, values: Map[String, String]) = {
    var counter = 0
    val start = System.currentTimeMillis
    var startTime = System.currentTimeMillis
    var finishTime = System.currentTimeMillis

    persistenceManager.pageOverAll(entity, (guid, map) => {
      counter += 1

      for (key <- values.keySet) {
        if (map.contains(key)) {
          persistenceManager.put(guid, entity, values.get(key).get, map.get(key).get)
          persistenceManager.deleteColumns(guid, entity, key)
        }
      }

      if (counter % 1000 == 0) {
        finishTime = System.currentTimeMillis
        println(counter + " >> Last key : " + guid + ", records per sec: " + 1000f / (((finishTime - startTime).toFloat) / 1000f))
        startTime = System.currentTimeMillis

      }

      true
    }, startUuid)

    finishTime = System.currentTimeMillis
    println("Total indexing time " + ((finishTime - start).toFloat) / 1000f + " seconds")
  }
}
