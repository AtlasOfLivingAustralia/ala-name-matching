package au.org.ala.sensitiveData.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.SensitivityCategory;

public class LookupListDaoImpl implements LookupDao {
	
	private static List<SensitiveSpecies > species = Arrays.asList(
			new SensitiveSpecies("Amytornis barbatus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Amytornis dorotheae", SensitivityCategory.HIGH),
			new SensitiveSpecies("Amytornis housei", SensitivityCategory.HIGH),
			new SensitiveSpecies("Amytornis woodwardi", SensitivityCategory.HIGH),
			new SensitiveSpecies("Anas eatoni", SensitivityCategory.HIGH),
			new SensitiveSpecies("Anthochaera phrygia", SensitivityCategory.HIGH),
			new SensitiveSpecies("Atrichornis clamosus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Atrichornis rufescens", SensitivityCategory.HIGH),
			new SensitiveSpecies("Botaurus poiciloptilus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Cacatua pastinator", SensitivityCategory.HIGH),
			new SensitiveSpecies("Calyptorhynchus banksii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Calyptorhynchus baudinii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Calyptorhynchus lathami", SensitivityCategory.HIGH),
			new SensitiveSpecies("Calyptorhynchus latirostris", SensitivityCategory.HIGH),
			new SensitiveSpecies("Casuarius casuarius", SensitivityCategory.HIGH),
			new SensitiveSpecies("Chionis minor", SensitivityCategory.HIGH),
			new SensitiveSpecies("Collocalia esculenta", SensitivityCategory.HIGH),
			new SensitiveSpecies("Conopophila whitei", SensitivityCategory.HIGH),
			new SensitiveSpecies("Crex crex", SensitivityCategory.HIGH),
			new SensitiveSpecies("Cyanoramphus cookii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Cyclopsitta diophthalma", SensitivityCategory.HIGH),
			new SensitiveSpecies("Dasyornis brachypterus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Dasyornis broadbenti", SensitivityCategory.HIGH),
			new SensitiveSpecies("Dasyornis longirostris", SensitivityCategory.HIGH),
			new SensitiveSpecies("Ducula whartoni", SensitivityCategory.HIGH),
			new SensitiveSpecies("Eclectus roratus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Elanus scriptus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Erythrotriorchis radiatus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Erythrura gouldiae", SensitivityCategory.HIGH),
			new SensitiveSpecies("Falco hypoleucos", SensitivityCategory.HIGH),
			new SensitiveSpecies("Falcunculus frontatus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Fregata andrewsi", SensitivityCategory.HIGH),
			new SensitiveSpecies("Fregetta tropica", SensitivityCategory.HIGH),
			new SensitiveSpecies("Gallus varius", SensitivityCategory.HIGH),
			new SensitiveSpecies("Geophaps scripta", SensitivityCategory.HIGH),
			new SensitiveSpecies("Geophaps smithii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Gerygone modesta", SensitivityCategory.HIGH),
			new SensitiveSpecies("Ixobrychus dubius", SensitivityCategory.HIGH),
			new SensitiveSpecies("Lathamus discolor", SensitivityCategory.HIGH),
			new SensitiveSpecies("Leipoa ocellata", SensitivityCategory.HIGH),
			new SensitiveSpecies("Lophochroa leadbeateri", SensitivityCategory.HIGH),
			new SensitiveSpecies("Lophoictinia isura", SensitivityCategory.HIGH),
			new SensitiveSpecies("Malurus coronatus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Manorina melanotis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Menura alberti", SensitivityCategory.HIGH),
			new SensitiveSpecies("Morus capensis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Neochmia phaeton", SensitivityCategory.HIGH),
			new SensitiveSpecies("Neochmia ruficauda", SensitivityCategory.HIGH),
			new SensitiveSpecies("Neophema chrysogaster", SensitivityCategory.HIGH),
			new SensitiveSpecies("Neophema pulchella", SensitivityCategory.HIGH),
			new SensitiveSpecies("Neophema splendida", SensitivityCategory.HIGH),
			new SensitiveSpecies("Ninox connivens", SensitivityCategory.HIGH),
			new SensitiveSpecies("Ninox rufa", SensitivityCategory.HIGH),
			new SensitiveSpecies("Ninox strenua", SensitivityCategory.HIGH),
			new SensitiveSpecies("Northiella haematogaster", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pachycephala rufogularis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Papasula abbotti", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pardalotus quadragintus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pedionomus torquatus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Petroica multicolor", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pezoporus occidentalis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Phaethon lepturus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Poephila cincta", SensitivityCategory.HIGH),
			new SensitiveSpecies("Polytelis alexandrae", SensitivityCategory.HIGH),
			new SensitiveSpecies("Polytelis anthopeplus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Polytelis swainsonii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Procelsterna cerulea", SensitivityCategory.HIGH),
			new SensitiveSpecies("Psephotus chrysopterygius", SensitivityCategory.HIGH),
			new SensitiveSpecies("Psephotus dissimilis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Psophodes nigrogularis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pterodroma cervicalis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pterodroma mollis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pterodroma occulta", SensitivityCategory.HIGH),
			new SensitiveSpecies("Pterodroma solandri", SensitivityCategory.HIGH),
			new SensitiveSpecies("Puffinus assimilis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Rostratula australis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Stipiturus mallee", SensitivityCategory.HIGH),
			new SensitiveSpecies("Sula dactylatra", SensitivityCategory.HIGH),
			new SensitiveSpecies("Tachybaptus ruficollis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Thinornis rubricollis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Turdus poliocephalus", SensitivityCategory.HIGH),
			new SensitiveSpecies("Turnix melanogaster", SensitivityCategory.HIGH),
			new SensitiveSpecies("Turnix olivii", SensitivityCategory.HIGH),
			new SensitiveSpecies("Tyto longimembris", SensitivityCategory.HIGH),
			new SensitiveSpecies("Tyto novaehollandiae", SensitivityCategory.HIGH),
			new SensitiveSpecies("Tyto tenebricosa", SensitivityCategory.HIGH),
			new SensitiveSpecies("Zosterops albogularis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Zosterops natalis", SensitivityCategory.HIGH),
			new SensitiveSpecies("Zosterops tenuirostris", SensitivityCategory.HIGH));
	
	public SensitiveSpecies findByName(String scientificName) {
		try {
			SensitiveSpecies ss = new SensitiveSpecies(scientificName, SensitivityCategory.NOT_SENSITIVE);
			int match = Collections.binarySearch(species, ss);
			if (species.get(match).equals(ss)) {
				return species.get(match);
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

}
