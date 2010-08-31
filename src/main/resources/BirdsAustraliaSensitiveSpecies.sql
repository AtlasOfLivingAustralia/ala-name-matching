use portal;

DROP TABLE IF EXISTS sensitive_species;

CREATE TABLE sensitive_species (
	id INTEGER(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
	common_name VARCHAR(50) NOT NULL,
	scientific_name VARCHAR(50) NOT NULL,
	family VARCHAR(50) NOT NULL,
	data_provider_id SMALLINT(5) UNSIGNED,
	sensitivity_category CHAR(1) NOT NULL);
	
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Abbott\'s Booby','Papasula abbotti','Sulidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Albert\'s Lyrebird','Menura alberti','Menuridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Australasian Bittern','Botaurus poiciloptilus','Ardeidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Australian Little Bittern','Ixobrychus dubius','Ardeidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Australian Painted Snipe','Rostratula australis','Rostratulidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Barking Owl','Ninox connivens','Strigidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Baudin\'s Black-Cockatoo','Calyptorhynchus baudinii','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black Grasswren','Amytornis housei','Maluridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black-bellied Storm-Petrel','Fregetta tropica','Hydrobatidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black-breasted Button-quail','Turnix melanogaster','Turnicidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black-eared Miner','Manorina melanotis','Meliphagidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black-faced Sheathbill','Chionis minor','Chionidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Black-throated Finch','Poephila cincta','Passeridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Blue Bonnet','Northiella haematogaster','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Buff-breasted Button-quail','Turnix olivii','Turnicidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Cape Gannet','Morus capensis','Sulidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Carnaby\'s Black-Cockatoo','Calyptorhynchus latirostris','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Carpentarian Grasswren','Amytornis dorotheae','Maluridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Christmas Island Frigatebird','Fregata andrewsi','Fregatidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Christmas Island Imperial-Pigeon','Ducula whartoni','Columbidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Christmas Island White-eye','Zosterops natalis','Zosteropidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Corncrake','Crex crex','Rallidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Crested Shrike-tit','Falcunculus frontatus','Pachycephalidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Crimson Finch','Neochmia phaeton','Passeridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Double-eyed Fig-Parrot','Cyclopsitta diophthalma','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Eastern Bristlebird','Dasyornis brachypterus','Pardalotidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Eastern Grass Owl','Tyto longimembris','Tytonidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Eclectus Parrot','Eclectus roratus','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Forty-spotted Pardalote','Pardalotus quadragintus','Pardalotidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Glossy Black-Cockatoo','Calyptorhynchus lathami','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Glossy Swiftlet','Collocalia esculenta','Apodidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Golden-shouldered Parrot','Psephotus chrysopterygius','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Gouldian Finch','Erythrura gouldiae','Passeridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Green Junglefowl','Gallus varius','Phasianidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Grey Falcon','Falco hypoleucos','Falconidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Grey Grasswren','Amytornis barbatus','Maluridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Grey Honeyeater','Conopophila whitei','Meliphagidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Grey Ternlet','Procelsterna cerulea','Laridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Hooded Parrot','Psephotus dissimilis','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Hooded Plover','Thinornis rubricollis','Charadriidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Island Thrush','Turdus poliocephalus','Muscicapidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Kerguelen Pintail','Anas eatoni','Anatidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Letter-winged Kite','Elanus scriptus','Accipitridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Little Grebe','Tachybaptus ruficollis','Podicipedidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Little Shearwater','Puffinus assimilis','Procellariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Major Mitchell\'s Cockatoo','Lophochroa leadbeateri','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Mallee Emu-wren','Stipiturus mallee','Maluridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Malleefowl','Leipoa ocellata','Megapodiidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Masked Booby','Sula dactylatra','Sulidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Masked Owl','Tyto novaehollandiae','Tytonidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Night Parrot','Pezoporus occidentalis','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Noisy Scrub-bird','Atrichornis clamosus','Atrichornithidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Norfolk Island Gerygone','Gerygone modesta','Pardalotidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Orange-bellied Parrot','Neophema chrysogaster','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Pacific Robin','Petroica multicolor','Petroicidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Partridge Pigeon','Geophaps smithii','Columbidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Plains-wanderer','Pedionomus torquatus','Pedionomidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Powerful Owl','Ninox strenua','Strigidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Princess Parrot','Polytelis alexandrae','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Providence Petrel','Pterodroma solandri','Procellariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Purple-crowned Fairy-wren','Malurus coronatus','Maluridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Red Goshawk','Erythrotriorchis radiatus','Accipitridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Red-lored Whistler','Pachycephala rufogularis','Pachycephalidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Red-tailed Black-Cockatoo','Calyptorhynchus banksii','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Regent Honeyeater','Anthochaera phrygia','Meliphagidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Regent Parrot','Polytelis anthopeplus','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Rufous Bristlebird','Dasyornis broadbenti','Pardalotidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Rufous Owl','Ninox rufa','Strigidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Rufous Scrub-bird','Atrichornis rufescens','Atrichornithidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Scarlet-chested Parrot','Neophema splendida','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Slender-billed White-eye','Zosterops tenuirostris','Zosteropidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Soft-plumaged Petrel','Pterodroma mollis','Procellariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Sooty Owl','Tyto tenebricosa','Tytonidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Southern Cassowary','Casuarius casuarius','Casuariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Square-tailed Kite','Lophoictinia isura','Accipitridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Squatter Pigeon','Geophaps scripta','Columbidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Star Finch','Neochmia ruficauda','Passeridae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Superb Parrot','Polytelis swainsonii','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Swift Parrot','Lathamus discolor','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Tasman Parakeet','Cyanoramphus cookii','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Turquoise Parrot','Neophema pulchella','Psittacidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Vanuatu Petrel','Pterodroma occulta','Procellariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Western Bristlebird','Dasyornis longirostris','Pardalotidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Western Corella','Cacatua pastinator','Cacatuidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('Western Whipbird','Psophodes nigrogularis','Psophodidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('White-chested White-eye','Zosterops albogularis','Zosteropidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('White-necked Petrel','Pterodroma cervicalis','Procellariidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('White-tailed Tropicbird','Phaethon lepturus','Phaethontidae',289,'H');
INSERT INTO sensitive_species (common_name,scientific_name,family,data_provider_id,sensitivity_category) VALUES ('White-throated Grasswren','Amytornis woodwardi','Maluridae',289,'H');

CREATE INDEX scientific_name_idx ON sensitive_species (scientific_name);