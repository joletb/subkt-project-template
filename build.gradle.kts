import myaa.subkt.ass.*
import myaa.subkt.tasks.*
import myaa.subkt.tasks.Mux.*
import myaa.subkt.tasks.Nyaa.*
import java.awt.Color
import java.time.*

plugins {
    id("myaa.subkt")
}

fun String.isKaraTemplate(): Boolean {
    return this.startsWith("code") || this.startsWith("template") || this.startsWith("mixin")
}

fun EventLine.isKaraTemplate(): Boolean {
    return this.comment && this.effect.isKaraTemplate()
}

subs {
    readProperties("sub.properties", "../sekrit.properties")
    release(arg("release") ?: "TV")
    episodes(getList("episodes"))
    batches(getMap("batches", "episodes"))

    merge {
        if (File(get("mergetemplate").get()).exists()) {
            fromMergeTemplate(get("mergetemplate"))
            } else {
                from(get("dialogue")) {
                incrementLayer(99)
            }

            if (propertyExists("OP")) {
                from(get("OP")) {
                    syncSourceLine("sync")
                    syncTargetLine("opsync")
                }
            }

            if (propertyExists("ED")) {
                from(get("ED")) {
                    syncSourceLine("sync")
                    syncTargetLine("edsync")
                }
            }

            fromIfPresent(get("extra"), ignoreMissingFiles = true)
            fromIfPresent(get("INS"), ignoreMissingFiles = true)
            fromIfPresent(getList("TS"), ignoreMissingFiles = true)

        includeExtraData(false)
        includeProjectGarbage(false)

        scriptInfo {
            title = get("group_full").get()
            scaledBorderAndShadow = true
            }
        }
    }

    val cleanmerge by task<ASS> {
        from(merge.item())
        ass {
            events.lines.removeIf { it.isKaraTemplate() }
        }
    }

    chapters {
        from(cleanmerge.item())
        chapterMarker("chapter")
    }

    swap { from(cleanmerge.item()) }

    mux {
        title(get("muxtitle"))

        skipUnusedFonts(true)

        from(get("premux")) {
            video {
                lang("jpn")
                default(true)
            }
            audio() {
                lang("jpn")
                default(true)
            }
            includeChapters(false)
            attachments { include(false) }
        }

        from(cleanmerge.item()) {
            subtitles {
                lang("eng")
                name(get("group_title"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        chapters(chapters.item()) { lang("eng") }

        attach(get("dialoguefonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        attach(get("fonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        if (propertyExists("OP")) {
            attach(get("opfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        if (propertyExists("ED")) {
            attach(get("edfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        out(get("muxout"))
    }

    alltasks {
        torrent {
            trackers(getList("trackers"))
            from(mux.batchItems())
            if (isBatch) {
                into(get("muxtitle_batch"))
                out(get("torrentfile_batch"))
            } else {
                out(get("torrentfile"))
            }
        }

        nyaa {
            from(torrent.item())
            username(get("torrentuser"))
            password(get("torrentpass"))
            category(NyaaCategories.ANIME_ENGLISH)
            hidden(false)
            information(get("torrentinfo"))
            torrentName(get("muxtitle_mkv"))
            if (isBatch) {
                torrentDescription(getFile("torrent_desc_nyaa_batch.txt"))
            } else {
                torrentDescription(getFile("torrent_desc_nyaa.txt"))
            }
        }

        fun SFTP.configure() {
            host(get("ftphost"))
            username(get("ftpuser"))
            password(get("ftppass"))
            port(getAs<Int>("ftpport"))
            knownHosts("../known_hosts")
            identity("../id_rsa")
        }

        val uploadFiles by task<SFTP> {
            from(mux.batchItems())
            configure()
            if (isBatch) {
                into(get("ftpfiledir_batch"))
            } else {
                into(get("ftpfiledir"))
            }
        }

        val checkFiles by task<SSHExec> {
            dependsOn(uploadFiles.item())
            host(get("sshhost"))
            username(get("sshusername"))
            port(getAs<Int>("sshport"))
            identity("../id_rsa")
            knownHosts("../known_hosts")
            if (isBatch) {
                command(get("crcCheck_batch"))
            } else {
                command(get("crcCheck"))
            }

            // TODO: Do ErrorMode.FAIL on CRC32 mismatch.
        }

        val startSeeding by task<SFTP> {
            // upload files to seedbox and publish to nyaa before initiating seeding
            dependsOn(uploadFiles.item(), nyaa.item())
            from(torrent.item())
            configure()
            into(get("ftptorrentdir"))
        }
    }
}
