use portal;

DROP TABLE IF EXISTS sensitive_species;

CREATE TABLE sensitive_species (
	id INTEGER(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
	common_name VARCHAR(50) NOT NULL,
	scientific_name VARCHAR(50) NOT NULL,
	family VARCHAR(50) NOT NULL,
	data_provider VARCHAR(255) NOT NULL,
	sensitivity_category CHAR(1) NOT NULL);
	
INSERT INTO sensitive_species VALUES ('Abbott\'s Booby','Papasula abbotti','Sulidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Albert\'s Lyrebird','Menura alberti','Menuridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Australasian Bittern','Botaurus poiciloptilus','Ardeidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Australian Little Bittern','Ixobrychus dubius','Ardeidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Australian Painted Snipe','Rostratula australis','Rostratulidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Barking Owl','Ninox connivens','Strigidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Baudin\'s Black-Cockatoo','Calyptorhynchus baudinii','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black Grasswren','Amytornis housei','Maluridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black-bellied Storm-Petrel','Fregetta tropica','Hydrobatidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black-breasted Button-quail','Turnix melanogaster','Turnicidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black-eared Miner','Manorina melanotis','Meliphagidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black-faced Sheathbill','Chionis minor','Chionidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Black-throated Finch','Poephila cincta','Passeridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Blue Bonnet','Northiella haematogaster','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Buff-breasted Button-quail','Turnix olivii','Turnicidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Cape Gannet','Morus capensis','Sulidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Carnaby\'s Black-Cockatoo','Calyptorhynchus latirostris','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Carpentarian Grasswren','Amytornis dorotheae','Maluridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Christmas Island Frigatebird','Fregata andrewsi','Fregatidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Christmas Island Imperial-Pigeon','Ducula whartoni','Columbidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Christmas Island White-eye','Zosterops natalis','Zosteropidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Corncrake','Crex crex','Rallidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Crested Shrike-tit','Falcunculus frontatus','Pachycephalidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Crimson Finch','Neochmia phaeton','Passeridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Double-eyed Fig-Parrot','Cyclopsitta diophthalma','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Eastern Bristlebird','Dasyornis brachypterus','Pardalotidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Eastern Grass Owl','Tyto longimembris','Tytonidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Eclectus Parrot','Eclectus roratus','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Forty-spotted Pardalote','Pardalotus quadragintus','Pardalotidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Glossy Black-Cockatoo','Calyptorhynchus lathami','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Glossy Swiftlet','Collocalia esculenta','Apodidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Golden-shouldered Parrot','Psephotus chrysopterygius','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Gouldian Finch','Erythrura gouldiae','Passeridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Green Junglefowl','Gallus varius','Phasianidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Grey Falcon','Falco hypoleucos','Falconidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Grey Grasswren','Amytornis barbatus','Maluridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Grey Honeyeater','Conopophila whitei','Meliphagidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Grey Ternlet','Procelsterna cerulea','Laridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Hooded Parrot','Psephotus dissimilis','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Hooded Plover','Thinornis rubricollis','Charadriidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Island Thrush','Turdus poliocephalus','Muscicapidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Kerguelen Pintail','Anas eatoni','Anatidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Letter-winged Kite','Elanus scriptus','Accipitridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Little Grebe','Tachybaptus ruficollis','Podicipedidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Little Shearwater','Puffinus assimilis','Procellariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Major Mitchell\'s Cockatoo','Lophochroa leadbeateri','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Mallee Emu-wren','Stipiturus mallee','Maluridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Malleefowl','Leipoa ocellata','Megapodiidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Masked Booby','Sula dactylatra','Sulidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Masked Owl','Tyto novaehollandiae','Tytonidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Night Parrot','Pezoporus occidentalis','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Noisy Scrub-bird','Atrichornis clamosus','Atrichornithidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Norfolk Island Gerygone','Gerygone modesta','Pardalotidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Orange-bellied Parrot','Neophema chrysogaster','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Pacific Robin','Petroica multicolor','Petroicidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Partridge Pigeon','Geophaps smithii','Columbidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Plains-wanderer','Pedionomus torquatus','Pedionomidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Powerful Owl','Ninox strenua','Strigidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Princess Parrot','Polytelis alexandrae','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Providence Petrel','Pterodroma solandri','Procellariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Purple-crowned Fairy-wren','Malurus coronatus','Maluridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Red Goshawk','Erythrotriorchis radiatus','Accipitridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Red-lored Whistler','Pachycephala rufogularis','Pachycephalidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Red-tailed Black-Cockatoo','Calyptorhynchus banksii','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Regent Honeyeater','Anthochaera phrygia','Meliphagidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Regent Parrot','Polytelis anthopeplus','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Rufous Bristlebird','Dasyornis broadbenti','Pardalotidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Rufous Owl','Ninox rufa','Strigidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Rufous Scrub-bird','Atrichornis rufescens','Atrichornithidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Scarlet-chested Parrot','Neophema splendida','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Slender-billed White-eye','Zosterops tenuirostris','Zosteropidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Soft-plumaged Petrel','Pterodroma mollis','Procellariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Sooty Owl','Tyto tenebricosa','Tytonidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Southern Cassowary','Casuarius casuarius','Casuariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Square-tailed Kite','Lophoictinia isura','Accipitridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Squatter Pigeon','Geophaps scripta','Columbidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Star Finch','Neochmia ruficauda','Passeridae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Superb Parrot','Polytelis swainsonii','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Swift Parrot','Lathamus discolor','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Tasman Parakeet','Cyanoramphus cookii','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Turquoise Parrot','Neophema pulchella','Psittacidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Vanuatu Petrel','Pterodroma occulta','Procellariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Western Bristlebird','Dasyornis longirostris','Pardalotidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Western Corella','Cacatua pastinator','Cacatuidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('Western Whipbird','Psophodes nigrogularis','Psophodidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('White-chested White-eye','Zosterops albogularis','Zosteropidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('White-necked Petrel','Pterodroma cervicalis','Procellariidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('White-tailed Tropicbird','Phaethon lepturus','Phaethontidae','Birds Australia','H');
INSERT INTO sensitive_species VALUES ('White-throated Grasswren','Amytornis woodwardi','Maluridae','Birds Australia','H');

INSERT INTO sensitive_species VALUES ('Swan galaxias','Galaxias fontanus','Galaxiidae','Tasmania','H');
INSERT INTO sensitive_species VALUES ('Spotted handfish','Brachionichthys hirsutus','Brachionichthyidae','Tasmania','H');
INSERT INTO sensitive_species VALUES ('King\'s lomatia','Lomatia tasmanica','Proteaceae','Tasmania','H');

INSERT INTO sensitive_species VALUES ('Swan galaxias','Galaxias fontanus','Galaxiidae','Tasmania','H');

CREATE INDEX scientific_name_idx ON sensitive_species (scientific_name);