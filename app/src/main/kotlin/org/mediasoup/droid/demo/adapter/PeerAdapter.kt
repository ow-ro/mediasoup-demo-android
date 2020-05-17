package org.mediasoup.droid.demo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.mediasoup.droid.demo.R
import org.mediasoup.droid.demo.adapter.PeerAdapter.PeerViewHolder
import org.mediasoup.droid.demo.view.PeerView
import org.mediasoup.droid.demo.vm.PeerProps
import org.mediasoup.droid.lib.RoomClient
import org.mediasoup.droid.lib.lv.RoomStore
import org.mediasoup.droid.lib.model.Peer
import timber.log.Timber

class PeerAdapter(private val store: RoomStore,
                  private val lifecycleOwner: LifecycleOwner,
                  private val roomClient: RoomClient
) : RecyclerView.Adapter<PeerViewHolder>() {
    private var peers: List<Peer> = emptyList()
    private var containerHeight = 0

    fun replacePeers(peers: List<Peer>) {
        this.peers = peers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        containerHeight = parent.height
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_remote_peer, parent, false)
        return PeerViewHolder(view, PeerProps((context as AppCompatActivity).application, store))
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        // update height
        val layoutParams = holder.peerView.layoutParams
        layoutParams.height = itemHeight
        holder.peerView.layoutParams = layoutParams
        // bind
        holder.bind(lifecycleOwner, roomClient, peers[position])
    }

    override fun getItemCount(): Int = peers.size

    private val itemHeight: Int
        get() {
            val itemCount = itemCount
            return when {
                itemCount <= 1 -> containerHeight
                itemCount <= 3 -> containerHeight / itemCount
                else -> (containerHeight / 3.2).toInt()
            }
        }

    class PeerViewHolder(view: View, private val peerProps: PeerProps) : ViewHolder(view) {
        internal val peerView: PeerView = view.findViewById(R.id.remote_peer)

        fun bind(owner: LifecycleOwner, roomClient: RoomClient, peer: Peer) {
            Timber.d("bind() id: %s, name: %s", peer.id, peer.displayName)
            peerProps.connect(owner, peer.id)
            peerView.setProps(peerProps, roomClient)
        }
    }
}