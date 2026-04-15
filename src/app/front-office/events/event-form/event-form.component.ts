import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Event } from '../models/event.model';
import { EventService } from '../services/event.service';
import { ForumService } from '../../forum/services/forum.service';
import { PostResponse } from '../../forum/models/forum.model';
import { isPlatformBrowser } from '@angular/common';


@Component({
    selector: 'app-event-form',
    templateUrl: './event-form.component.html',
    styleUrls: ['./event-form.component.scss']
})
export class EventFormComponent implements OnInit {
    event: Event = {
        name: '',
        description: '',
        details: '',
        location: '',
        latitude: 0,
        longitude: 0,
        imageUrl: '',
        date: new Date(),
        attendees: 0,
        category: 'Technology',
        createdAt: new Date()
    };

    isEditMode = false;
    eventId: number | null = null;
    loading = false;
    imagePreview: string | null = null;
    forumTopics: PostResponse[] = [];
    loadingTopics = false;

    categories = ['Technology', 'Education', 'Business', 'Health', 'Arts', 'Sports', 'Other'];

    private map: any = null;
    private marker: any = null;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private eventService: EventService,
        private forumService: ForumService,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.loadForumTopics();
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.isEditMode = true;
            this.eventId = Number(id);
            this.loadEvent();
        } else {
            // Create mode: initialize map with a default center (Tunis)
            this.initMap();
        }
    }

    loadForumTopics(): void {
        this.loadingTopics = true;
        this.forumService.getForums().subscribe({
            next: (topics) => {
                this.forumTopics = topics;
                this.loadingTopics = false;
            },
            error: (error) => {
                console.error('Error loading forum topics:', error);
                this.loadingTopics = false;
            }
        });
    }

    loadEvent(): void {
        if (this.eventId) {
            this.loading = true;
            this.eventService.getEventById(this.eventId).subscribe({
                next: (event) => {
                    this.event = event;
                    this.imagePreview = event.imageUrl || null;
                    this.loading = false;
                    this.initMap();
                },
                error: (error) => {
                    console.error('Error loading event:', error);
                    this.loading = false;
                }
            });
        }
    }

    saveEvent(): void {
        if (!this.validateForm()) {
            return;
        }

        this.loading = true;

        if (this.isEditMode && this.eventId) {
            this.eventService.updateEvent(this.eventId, this.event).subscribe({
                next: () => {
                    this.router.navigate(['/events', this.eventId]);
                },
                error: (error) => {
                    console.error('Error updating event:', error);
                    this.loading = false;
                }
            });
        } else {
            this.eventService.createEvent(this.event).subscribe({
                next: (newEvent) => {
                    this.router.navigate(['/events', newEvent.id]);
                },
                error: (error) => {
                    console.error('Error creating event:', error);
                    this.loading = false;
                }
            });
        }
    }

    validateForm(): boolean {
        if (!this.event.name || !this.event.description || !this.event.details || !this.event.location) {
            alert('Please fill in all required fields');
            return false;
        }
        if (!this.event.latitude || !this.event.longitude) {
            alert('Please select the event location on the map');
            return false;
        }
        if (this.event.attendees < 0) {
            alert('Attendees must be a positive number');
            return false;
        }
        return true;
    }

    cancel(): void {
        if (this.isEditMode && this.eventId) {
            this.router.navigate(['/events', this.eventId]);
        } else {
            this.router.navigate(['/events']);
        }
    }

    onImageSelected(event: any): void {
        const file = event.target.files[0];
        if (file) {
            if (!file.type.startsWith('image/')) {
                alert('Please select an image file');
                return;
            }

            if (file.size > 5 * 1024 * 1024) {
                alert('Image size should be less than 5MB');
                return;
            }

            const reader = new FileReader();
            reader.onload = (e: any) => {
                this.imagePreview = e.target.result;
                this.event.imageUrl = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    }

    removeImage(): void {
        this.imagePreview = null;
        this.event.imageUrl = '';
    }

    private async initMap(): Promise<void> {
        if (!isPlatformBrowser(this.platformId) || this.map) return;

        const mapEl = document.getElementById('osm-picker');
        if (!mapEl) {
            // Template might not be rendered yet; try again shortly
            setTimeout(() => this.initMap(), 100);
            return;
        }

        const L = await import('leaflet');

        // Fix default marker icons when bundling with Angular (use CDN so no assets are required)
        const iconRetinaUrl = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png';
        const iconUrl = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png';
        const shadowUrl = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png';
        (L as any).Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl });

        const lat = this.event.latitude || 36.8065;
        const lng = this.event.longitude || 10.1815;

        this.map = L.map(mapEl).setView([lat, lng], 12);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(this.map);

        this.marker = L.marker([lat, lng], { draggable: true }).addTo(this.map);

        const setCoords = (newLat: number, newLng: number) => {
            this.event.latitude = Number(newLat.toFixed(6));
            this.event.longitude = Number(newLng.toFixed(6));
        };

        this.map.on('click', (e: any) => {
            const { lat: clickLat, lng: clickLng } = e.latlng;
            this.marker.setLatLng([clickLat, clickLng]);
            setCoords(clickLat, clickLng);
        });

        this.marker.on('dragend', () => {
            const pos = this.marker.getLatLng();
            setCoords(pos.lat, pos.lng);
        });

        // Make sure fields are populated even if user doesn't move the marker
        setCoords(lat, lng);

        // Avoid grey tiles due to container size changes (cards, modals, etc.)
        setTimeout(() => this.map?.invalidateSize?.(), 0);
    }
}
