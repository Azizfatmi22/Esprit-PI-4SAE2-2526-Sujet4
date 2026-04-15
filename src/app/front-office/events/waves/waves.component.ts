import { Component, ElementRef, Input, OnInit, OnDestroy, ViewChild, AfterViewInit, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
    selector: 'app-waves',
    template: '<canvas #canvasContainer></canvas>',
    styles: [`
    canvas {
      width: 100%;
      height: 100%;
      display: block;
    }
  `]
})
export class WavesComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild('canvasContainer') canvasRef!: ElementRef<HTMLCanvasElement>;

    @Input() lineColor: string = '#29d4ff';
    @Input() backgroundColor: string = 'transparent';
    @Input() waveSpeedX: number = 0.02;
    @Input() waveSpeedY: number = 0.01;
    @Input() waveAmpX: number = 40;
    @Input() waveAmpY: number = 20;
    @Input() friction: number = 0.9;
    @Input() tension: number = 0.01;
    @Input() maxCursorMove: number = 120;
    @Input() xGap: number = 12;
    @Input() yGap: number = 36;

    private ctx!: CanvasRenderingContext2D;
    private animationId: number = 0;
    private points: any[] = [];
    private mouse = { x: -1000, y: -1000 };

    constructor(@Inject(PLATFORM_ID) private platformId: Object) { }

    ngOnInit(): void { }

    ngAfterViewInit(): void {
        if (isPlatformBrowser(this.platformId)) {
            this.initCanvas();
            this.animate();
            window.addEventListener('resize', this.onResize.bind(this));
            window.addEventListener('mousemove', this.onMouseMove.bind(this));
        }
    }

    ngOnDestroy(): void {
        if (isPlatformBrowser(this.platformId)) {
            cancelAnimationFrame(this.animationId);
            window.removeEventListener('resize', this.onResize.bind(this));
            window.removeEventListener('mousemove', this.onMouseMove.bind(this));
        }
    }

    private initCanvas(): void {
        const canvas = this.canvasRef.nativeElement;
        this.ctx = canvas.getContext('2d')!;
        this.onResize();
        this.createPoints();
    }

    private onResize(): void {
        const canvas = this.canvasRef.nativeElement;
        const parent = canvas.parentElement;
        if (parent) {
            canvas.width = parent.clientWidth;
            canvas.height = parent.clientHeight;
            this.createPoints();
        }
    }

    private onMouseMove(e: MouseEvent): void {
        const canvas = this.canvasRef.nativeElement;
        const rect = canvas.getBoundingClientRect();
        this.mouse.x = e.clientX - rect.left;
        this.mouse.y = e.clientY - rect.top;
    }

    private createPoints(): void {
        const canvas = this.canvasRef.nativeElement;
        this.points = [];
        for (let x = 0; x < canvas.width + this.xGap; x += this.xGap) {
            const line = [];
            for (let y = 0; y < canvas.height + this.yGap; y += this.yGap) {
                line.push({
                    x: x,
                    y: y,
                    originalX: x,
                    originalY: y,
                    vx: 0,
                    vy: 0
                });
            }
            this.points.push(line);
        }
    }

    private animate(): void {
        const canvas = this.canvasRef.nativeElement;
        this.ctx.clearRect(0, 0, canvas.width, canvas.height);

        if (this.backgroundColor !== 'transparent') {
            this.ctx.fillStyle = this.backgroundColor;
            this.ctx.fillRect(0, 0, canvas.width, canvas.height);
        }

        this.ctx.strokeStyle = this.lineColor;
        this.ctx.lineWidth = 1;

        const time = Date.now() * 0.001;

        for (let i = 0; i < this.points.length; i++) {
            const line = this.points[i];
            this.ctx.beginPath();
            for (let j = 0; j < line.length; j++) {
                const p = line[j];

                // Base wave movement
                const waveX = Math.sin(time * this.waveSpeedX + p.originalY * 0.01) * this.waveAmpX;
                const waveY = Math.cos(time * this.waveSpeedY + p.originalX * 0.01) * this.waveAmpY;

                const targetX = p.originalX + waveX;
                const targetY = p.originalY + waveY;

                // Interactive movement
                const dx = this.mouse.x - p.x;
                const dy = this.mouse.y - p.y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < this.maxCursorMove) {
                    const force = (this.maxCursorMove - dist) / this.maxCursorMove;
                    p.vx -= dx * force * 0.03;
                    p.vy -= dy * force * 0.03;
                }

                p.vx += (targetX - p.x) * this.tension;
                p.vy += (targetY - p.y) * this.tension;
                p.vx *= this.friction;
                p.vy *= this.friction;

                p.x += p.vx;
                p.y += p.vy;

                if (j === 0) {
                    this.ctx.moveTo(p.x, p.y);
                } else {
                    this.ctx.lineTo(p.x, p.y);
                }
            }
            this.ctx.stroke();

            // Horizontal lines
            if (i > 0) {
                this.ctx.beginPath();
                for (let j = 0; j < line.length; j++) {
                    const p1 = this.points[i - 1][j];
                    const p2 = line[j];
                    this.ctx.moveTo(p1.x, p1.y);
                    this.ctx.lineTo(p2.x, p2.y);
                }
                this.ctx.stroke();
            }
        }

        this.animationId = requestAnimationFrame(this.animate.bind(this));
    }
}
