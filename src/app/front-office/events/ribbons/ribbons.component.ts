import { Component, ElementRef, Input, OnInit, OnDestroy, ViewChild, AfterViewInit, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';

interface Point {
    x: number;
    y: number;
    vx: number;
    vy: number;
    age: number;
}

interface Ribbon {
    points: Point[];
    color: string;
    thickness: number;
}

@Component({
    selector: 'app-ribbons',
    standalone: true,
    imports: [CommonModule],
    template: '<canvas #canvasContainer></canvas>',
    styles: [`
    canvas {
      width: 100%;
      height: 100%;
      display: block;
    }
  `]
})
export class RibbonsComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild('canvasContainer') canvasRef!: ElementRef<HTMLCanvasElement>;

    @Input() colors: string[] = ["#FC8EAC"];
    @Input() baseSpring: number = 0.03;
    @Input() baseFriction: number = 0.9;
    @Input() baseThickness: number = 30;
    @Input() offsetFactor: number = 0.05;
    @Input() maxAge: number = 500;
    @Input() pointCount: number = 50;
    @Input() speedMultiplier: number = 0.6;
    @Input() enableFade: boolean = false;
    @Input() enableShaderEffect: boolean = false;
    @Input() effectAmplitude: number = 2;

    private ctx!: CanvasRenderingContext2D;
    private animationId: number = 0;
    private ribbons: Ribbon[] = [];
    private mouse = { x: -1000, y: -1000 };
    private isMouseMoving = false;

    constructor(@Inject(PLATFORM_ID) private platformId: Object) { }

    ngOnInit(): void { }

    ngAfterViewInit(): void {
        if (isPlatformBrowser(this.platformId)) {
            this.initCanvas();
            this.animate();
            window.addEventListener('resize', this.handleResize.bind(this));
            window.addEventListener('mousemove', this.handleMouseMove.bind(this));
        }
    }

    ngOnDestroy(): void {
        if (isPlatformBrowser(this.platformId)) {
            cancelAnimationFrame(this.animationId);
            window.removeEventListener('resize', this.handleResize.bind(this));
            window.removeEventListener('mousemove', this.handleMouseMove.bind(this));
        }
    }

    private initCanvas(): void {
        const canvas = this.canvasRef.nativeElement;
        this.ctx = canvas.getContext('2d')!;
        this.handleResize();
        this.createRibbons();
    }

    private handleResize(): void {
        const canvas = this.canvasRef.nativeElement;
        const parent = canvas.parentElement;
        if (parent) {
            canvas.width = parent.clientWidth;
            canvas.height = parent.clientHeight;
        }
    }

    private handleMouseMove(e: MouseEvent): void {
        const canvas = this.canvasRef.nativeElement;
        const rect = canvas.getBoundingClientRect();
        this.mouse.x = e.clientX - rect.left;
        this.mouse.y = e.clientY - rect.top;
        this.isMouseMoving = true;
    }

    private createRibbons(): void {
        this.ribbons = [];
        const colorCount = this.colors.length;
        for (let i = 0; i < 3; i++) {
            this.ribbons.push({
                points: [],
                color: this.colors[i % colorCount],
                thickness: this.baseThickness * (1 - i * 0.2)
            });
        }
    }

    private animate(): void {
        this.ctx.clearRect(0, 0, this.canvasRef.nativeElement.width, this.canvasRef.nativeElement.height);

        if (this.isMouseMoving) {
            this.ribbons.forEach((ribbon, i) => {
                const targetX = this.mouse.x + Math.sin(Date.now() * 0.001 + i) * 20;
                const targetY = this.mouse.y + Math.cos(Date.now() * 0.001 + i) * 20;

                if (ribbon.points.length === 0) {
                    ribbon.points.push({ x: targetX, y: targetY, vx: 0, vy: 0, age: 0 });
                } else {
                    const head = ribbon.points[0];
                    head.vx += (targetX - head.x) * this.baseSpring;
                    head.vy += (targetY - head.y) * this.baseSpring;
                    head.vx *= this.baseFriction;
                    head.vy *= this.baseFriction;
                    head.x += head.vx * this.speedMultiplier;
                    head.y += head.vy * this.speedMultiplier;
                }

                // Update followers
                for (let j = 1; j < this.pointCount; j++) {
                    if (j >= ribbon.points.length) {
                        const prev = ribbon.points[j - 1];
                        ribbon.points.push({ x: prev.x, y: prev.y, vx: 0, vy: 0, age: 0 });
                    } else {
                        const p = ribbon.points[j];
                        const prev = ribbon.points[j - 1];
                        p.vx += (prev.x - p.x) * this.baseSpring * 2;
                        p.vy += (prev.y - p.y) * this.baseSpring * 2;
                        p.vx *= this.baseFriction;
                        p.vy *= this.baseFriction;
                        p.x += p.vx * this.speedMultiplier;
                        p.y += p.vy * this.speedMultiplier;
                    }
                }
            });
        }

        // Draw ribbons
        this.ribbons.forEach(ribbon => {
            if (ribbon.points.length < 2) return;

            this.ctx.beginPath();
            this.ctx.moveTo(ribbon.points[0].x, ribbon.points[0].y);

            for (let i = 1; i < ribbon.points.length - 2; i++) {
                const xc = (ribbon.points[i].x + ribbon.points[i + 1].x) / 2;
                const yc = (ribbon.points[i].y + ribbon.points[i + 1].y) / 2;
                this.ctx.quadraticCurveTo(ribbon.points[i].x, ribbon.points[i].y, xc, yc);
            }

            this.ctx.strokeStyle = ribbon.color;
            this.ctx.lineWidth = ribbon.thickness;
            this.ctx.lineCap = 'round';
            this.ctx.lineJoin = 'round';
            this.ctx.stroke();
        });

        this.animationId = requestAnimationFrame(this.animate.bind(this));
    }
}
